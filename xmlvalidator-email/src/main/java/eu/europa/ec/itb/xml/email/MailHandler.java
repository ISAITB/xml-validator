package eu.europa.ec.itb.xml.email;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.upload.FileController;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.FileReport;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by simatosc on 16/03/2016.
 */
@Component
public class MailHandler {

    private static Logger logger = LoggerFactory.getLogger(MailHandler.class);

    private Map<String, JavaMailSender> mailSenders = new ConcurrentHashMap<>();

    @Autowired
    FileManager fileManager;
    @Autowired
    FileController fileController;
    @Autowired
    DomainConfigCache domainConfigCache;
    @Autowired
    BeanFactory beans;
    @Autowired
    ApplicationConfig appConfig;

    @PostConstruct
    public void init() {
        // Initialize email-specific configuration.
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.EMAIL)) {
                mailSenders.put(domainConfig.getDomain(), createJavaMailSender(domainConfig));
                logger.info("Configured mail service for ["+domainConfig.getDomainName()+"]");
            }
        }
        if (mailSenders.isEmpty()) {
            logger.info("No email channels configured");
        }
    }

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


    @Scheduled(fixedDelayString = "${validator.mailPollingRate}")
    public void receiveEmail() {
        for (DomainConfig config: domainConfigCache.getAllDomainConfigurations()) {
            if (config.getChannels().contains(ValidatorChannel.EMAIL)) {
                try {
                    MDC.put("domain", config.getDomainName());
                    logger.info("Checking emails for ["+config.getDomainName()+"]...");
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
                                    if (contentObj instanceof Multipart) {
                                        logger.info("Processing message ["+message.getSubject()+"]");
                                        Multipart content = (Multipart)contentObj;
                                        for (int i=0; i < content.getCount(); i++) {
                                            BodyPart part = content.getBodyPart(i);
                                            if (!StringUtils.isBlank(part.getFileName())) {
                                                boolean acceptableFileType = false;
                                                try (InputStream is = part.getInputStream()) {
                                                    acceptableFileType = checkFileType(is);
                                                }
                                                if (acceptableFileType) {
                                                    String validationType = getValidationType(part.getFileName(), config);
                                                    logger.info("Processing file ["+part.getFileName()+"] of ["+message.getSubject()+"] for ["+validationType+"]");
                                                    try (InputStream is = part.getInputStream()) {
                                                        XMLValidator validator = beans.getBean(XMLValidator.class, is, validationType, config);
                                                        TAR report = validator.validateAll();
                                                        reports.add(new FileReport(part.getFileName(), report));
                                                        logger.info("Processed message ["+message.getSubject()+"], file ["+part.getFileName()+"]");
                                                    } catch (Exception e) {
                                                        messageAdditionalText.append("Failed to validate file ["+part.getFileName()+"]: "+e.getMessage()+"\n");
                                                        logger.warn("Failed to validate file ["+part.getFileName()+"]", e);
                                                    }
                                                } else {
                                                    logger.info("Ignoring file ["+part.getFileName()+"] of ["+message.getSubject()+"]");
                                                }
                                            }
                                        }
                                        if (reports.isEmpty()) {
                                            String msg = "No reports to send for ["+message.getSubject()+"]";
                                            messageAdditionalText.append(msg);
                                            logger.info(msg);
                                        }
                                    } else {
                                        String msg = "Skipping message ["+message.getSubject()+"] as non-multipart";
                                        messageAdditionalText.append(msg);
                                        logger.info(msg);
                                    }
                                } catch (Exception e) {
                                    // Send error response to sender.
                                    messageAdditionalText.append("Failed to process message ["+message.getSubject()+"]: "+e.getMessage()+"\n");
                                    e.printStackTrace(new PrintWriter(new StringBuilderWriter(messageAdditionalText)));
                                } finally {
                                    logger.info("Sending email response for ["+message.getSubject()+"]");
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
                            }
                        }
                        if (store != null) {
                            try {
                                store.close();
                            } catch (MessagingException e) {
                            }
                        }
                    }
                    logger.info("Checking emails completed for ["+config.getDomainName()+"].");
                } finally {
                    MDC.clear();
                }
            }
        }
    }

    private String getValidationType(String fileName, DomainConfig domainConfig) {
        /*
         The invoice type is determined from the file name's prefix. The format of the filename
         is as follows: [VALIDATION_TYPE].[NAME].[EXT]
         If wanting to target invoice type "abc" the file name would e.g. be "abc.originalName.xml".
         If there is only one type of invoice supported then the prefix can be omitted.
         */
        String validationType;
        if (domainConfig.hasMultipleValidationTypes()) {
            String prefix = fileName.substring(0, fileName.indexOf('.'));
            if (domainConfig.getType().contains(prefix)) {
                validationType = prefix;
            } else {
                throw new IllegalStateException("Validation type ["+prefix+"] determined for file ["+fileName+"] is not supported");
            }
        } else {
            validationType = domainConfig.getType().get(0);
        }
        return validationType;
    }

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
                helper.addAttachment(report.getReportXmlFileName(), fileController.getReportXml(domainConfig.getDomainName(), fileID));
                helper.addAttachment(report.getReportPdfFileName(), fileController.getReportPdf(domainConfig.getDomainName(), fileID));
                sb.append(report.toString()).append("\n\n");
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

    boolean checkFileType(InputStream is) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(is);
        return appConfig.getAcceptedMimeTypes().contains(type);
    }

}
