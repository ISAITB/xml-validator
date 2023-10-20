package eu.europa.ec.itb.xml.email;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.*;
import eu.europa.ec.itb.xml.upload.FileController;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component for the triggering of validation based on received emails.
 */
@Component
public class MailHandler {

    private static final Logger logger = LoggerFactory.getLogger(MailHandler.class);

    private final Map<String, JavaMailSender> mailSenders = new ConcurrentHashMap<>();

    @Autowired
    FileManager fileManager;
    @Autowired
    FileController fileController;
    @Autowired
    DomainConfigCache domainConfigCache;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    ApplicationConfig appConfig;
    @Autowired
    InputHelper inputHelper;

    /**
     * Initialize email-specific configuration.
     */
    @PostConstruct
    public void init() {
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.EMAIL)) {
                mailSenders.put(domainConfig.getDomain(), createJavaMailSender(domainConfig));
                logger.info("Configured mail service for [{}]", domainConfig.getDomainName());
            }
        }
        if (mailSenders.isEmpty()) {
            logger.info("No email channels configured");
        }
    }

    /**
     * Create an email sender using the current configuration properties.
     *
     * @param domainConfig The domain configuration.
     * @return The sender.
     */
    private JavaMailSender createJavaMailSender(DomainConfig domainConfig) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(domainConfig.getMailOutboundHost());
        impl.setPort(domainConfig.getMailOutboundPort());
        Properties props = new Properties();
        if (domainConfig.isMailAuthEnable()) {
            props.setProperty("mail.smtp.auth", "true");
            impl.setUsername(domainConfig.getMailAuthUsername());
            impl.setPassword(domainConfig.getMailAuthPassword());
        }
        if (domainConfig.isMailOutboundSSLEnable()) {
            props.setProperty("mail.smtp.ssl.enable", "true");
        }
        impl.setJavaMailProperties(props);
        return impl;
    }

    /**
     * Check for new emails.
     *
     * This method can be triggered manually but is otherwise fired at fixed intervals.
     */
    @Scheduled(fixedDelayString = "${validator.mailPollingRate}")
    public void receiveEmail() {
        for (DomainConfig config: domainConfigCache.getAllDomainConfigurations()) {
            if (config.getChannels().contains(ValidatorChannel.EMAIL)) {
                try {
                    MDC.put("domain", config.getDomainName());
                    logger.info("Checking emails for [{}]...", config.getDomainName());
                    Properties props = new Properties();
                    String storeName = "imap";
                    if (config.isMailInboundSSLEnable()) {
                        props.setProperty("mail.imap.ssl.enable", "true");
                        storeName = "imaps";
                    }
                    props.setProperty("mail.store.protocol", storeName);
                    Session session = Session.getInstance(props);
                    Store store = null;
                    Folder folder = null;
                    try {
                        store = session.getStore(storeName);
                        String username = null;
                        String password = null;
                        if (config.isMailAuthEnable()) {
                            username = config.getMailAuthUsername();
                            password = config.getMailAuthPassword();
                        }
                        store.connect(config.getMailInboundHost(), config.getMailInboundPort(), username, password);
                        folder = store.getFolder(config.getMailInboundFolder());
                        folder.open(Folder.READ_WRITE);
                        for (Message message: folder.getMessages()) {
                            List<FileReport> reports = new ArrayList<>();
                            try {
                                Object contentObj = message.getContent();
                                StringBuilder messageAdditionalText = new StringBuilder();
                                try {
                                    if (contentObj instanceof Multipart content) {
                                        for (int i=0; i < content.getCount(); i++) {
                                            BodyPart part = content.getBodyPart(i);
                                            if (!StringUtils.isBlank(part.getFileName())) {
                                                boolean acceptableFileType;
                                                try (InputStream is = part.getInputStream()) {
                                                    acceptableFileType = checkFileType(is);
                                                }
                                                if (acceptableFileType) {
                                                    String fileName = part.getFileName();
                                                    String validationType = inputHelper.validateValidationType(config, fileName.substring(0, fileName.indexOf('.')));
                                                    try (InputStream is = part.getInputStream()) {
                                                        var input = Files.createTempFile("itb-", "-input");
                                                        IOUtils.copy(is, Files.newOutputStream(input));
                                                        ValidationSpecs specs = ValidationSpecs.builder(input.toFile(), new LocalisationHelper(config, Locale.ENGLISH), config, applicationContext).withValidationType(validationType).build();
                                                        XMLValidator validator = applicationContext.getBean(XMLValidator.class, specs);
                                                        TAR report = validator.validateAll();
                                                        reports.add(new FileReport(part.getFileName(), report));
                                                    } catch (Exception e) {
                                                        messageAdditionalText.append("Failed to validate file [%s]: %s\n".formatted(part.getFileName(), e.getMessage()));
                                                        logger.warn("Failed to validate file", e);
                                                    }
                                                } else {
                                                    logger.info("Ignoring file.");
                                                }
                                            }
                                        }
                                        if (reports.isEmpty()) {
                                            String msg = "No reports to send for [%s]".formatted(message.getSubject());
                                            messageAdditionalText.append(msg);
                                            logger.info(msg);
                                        }
                                    } else {
                                        String msg = "Skipping message [%s] as non-multipart".formatted(message.getSubject());
                                        messageAdditionalText.append(msg);
                                        logger.info(msg);
                                    }
                                } catch (ValidatorException e) {
                                    // Send error response to sender.
                                    messageAdditionalText.append("Failed to process message: %s\n".formatted(e.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH))));
                                    e.printStackTrace(new PrintWriter(new StringBuilderWriter(messageAdditionalText)));
                                } catch (Exception e) {
                                    // Send error response to sender.
                                    messageAdditionalText.append("Failed to process message: %s\n".formatted(e.getMessage()));
                                    e.printStackTrace(new PrintWriter(new StringBuilderWriter(messageAdditionalText)));
                                } finally {
                                    logger.info("Sending email response");
                                    try {
                                        sendEmail(message, reports, messageAdditionalText.toString(), config);
                                    } catch(MessagingException e) {
                                        logger.error("Failed to send email response", e);
                                    } finally {
                                        message.setFlag(Flags.Flag.DELETED, true);
                                    }
                                }
                            } catch (MessagingException e) {
                                logger.error("Failed to read email messages", e);
                            } catch (IOException e) {
                                logger.error("Failed to read input and write report", e);
                            }
                        }
                    } catch (MessagingException e) {
                        logger.error("Unable to connect to IMAP server", e);
                        throw new IllegalStateException("Unable to connect to IMAP server", e);
                    } finally {
                        if (folder != null) {
                            try {
                                folder.close(true);
                            } catch (MessagingException e) {
                                // No action.
                            }
                        }
                        if (store != null) {
                            try {
                                store.close();
                            } catch (MessagingException e) {
                                // No action.
                            }
                        }
                    }
                    logger.info("Checking emails completed for [{}].", config.getDomainName());
                } finally {
                    MDC.clear();
                }
            }
        }
    }

    /**
     * Send the email response with the produced validation reports as attachments.
     *
     * @param inputMessage The message content.
     * @param reports The validation reports to include.
     * @param messageAdditionalText The additional text to add to the email's body.
     * @param domainConfig The domain configuration.
     * @throws MessagingException If an error occurs while sending the response.
     */
    public void sendEmail(Message inputMessage, Collection<FileReport> reports, String messageAdditionalText, DomainConfig domainConfig) throws MessagingException {
        JavaMailSender mailSender = mailSenders.get(domainConfig.getDomain());
        MimeMessage message = mailSender.createMimeMessage();
        List<String> idsToDelete = new ArrayList<>();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String to = inputMessage.getFrom()[0].toString();
            helper.setTo(to);
            helper.setFrom(domainConfig.getMailFrom());
            helper.setSubject("Validation report ["+inputMessage.getSubject()+"]");
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isBlank(messageAdditionalText)) {
                sb.append(messageAdditionalText).append("\n\n");
            }
            for (FileReport report: reports) {
                String fileID = UUID.randomUUID().toString();
                fileManager.saveReport(report.getReport(), fileID, domainConfig);
                helper.addAttachment(report.getReportXmlFileName(), fileController.getReportXml(domainConfig.getDomainName(), fileID, false, null));
                helper.addAttachment(report.getReportPdfFileName(), fileController.getReportPdf(domainConfig.getDomainName(), fileID, false, null, null));
                sb.append(report).append("\n\n");
            }
            helper.setText(sb.toString());
            mailSender.send(message);
            logger.info("Email sent to ["+to+"] for ["+inputMessage.getSubject()+"]");
        } catch (MessagingException e) {
            logger.error("Failed to send email message", e);
            throw e;
        }
        idsToDelete.parallelStream().forEach(id -> fileController.deleteReport(domainConfig.getDomainName(), id));
    }

    /**
     * Check to see if the provided attachment is of acceptable type for processing.
     *
     * @param is The attachment's input stream.
     * @return The check result.
     * @throws IOException If an error occurs during the check.
     */
    boolean checkFileType(InputStream is) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(is);
        return appConfig.getAcceptedMimeTypes().contains(type);
    }

}
