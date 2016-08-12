package eu.europa.ec.itb.einvoice.email;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.upload.FileController;
import eu.europa.ec.itb.einvoice.validation.FileReport;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by simatosc on 16/03/2016.
 */
@Component
@Profile("email")
public class MailHandler {

    private static Logger logger = LoggerFactory.getLogger(MailHandler.class);

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    FileController fileController;

    @Autowired
    ApplicationConfig config;

    @Autowired
    BeanFactory beans;

    @Scheduled(fixedDelayString = "${validator.mailPollingRate}")
    @Profile("email")
    public void receiveEmail() {
        logger.info("Checking emails...");
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        if (config.isMailInboundSSLEnable()) {
            props.setProperty("mail.imap.ssl.enable", "true");
        }
        Session session = Session.getInstance(props);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore("imaps");
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
                                boolean acceptableFileType = false;
                                try (InputStream is = part.getInputStream()) {
                                    acceptableFileType = checkFileType(is);
                                }
                                if (acceptableFileType) {
                                    String validationType = getValidationType(part.getFileName());
                                    logger.info("Processing file ["+part.getFileName()+"] of ["+message.getSubject()+"] for ["+validationType+"]");
                                    try (InputStream is = part.getInputStream()) {
                                        XMLValidator validator = beans.getBean(XMLValidator.class, is, validationType);
                                        TAR report = validator.validateAll();
                                        reports.add(new FileReport(part.getFileName(), report));
                                        logger.info("Processed message ["+message.getSubject()+"], file ["+part.getFileName()+"]");
                                    }
                                } else {
                                    logger.info("Ignoring file ["+part.getFileName()+"] of ["+message.getSubject()+"]");
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
                            sendEmail(message, reports, messageAdditionalText.toString());
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
        logger.info("Checking emails completed.");
    }

    private String getValidationType(String fileName) {
        /*
         The validation type is determined from the file name's prefix. The format of the filename
         is as follows: [VALIDATION_TYPE].[NAME].[EXT]
         If wanting to target validation type "abc" the file name would e.g. be "abc.originalName.xml".
         If there is only one type of validation supported then the prefix can be omitted.
         */
        String validationType;
        if (config.hasMultipleValidationTypes()) {
            String prefix = fileName.substring(0, fileName.indexOf('.'));
            if (config.getType().contains(prefix)) {
                validationType = prefix;
            } else {
                throw new IllegalStateException("Validation type ["+prefix+"] determined for file ["+fileName+"] is not supported");
            }
        } else {
            validationType = config.getType().get(0);
        }
        return validationType;
    }

    public void sendEmail(Message inputMessage, Collection<FileReport> reports, String messageAdditionalText) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        List<String> idsToDelete = new ArrayList<>();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String to = inputMessage.getFrom()[0].toString();
            helper.setTo(to);
            helper.setFrom(config.getMailFrom());
            helper.setSubject("Validation report ["+inputMessage.getSubject()+"]");
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isBlank(messageAdditionalText)) {
                sb.append(messageAdditionalText).append("\n\n");
            }
            for (FileReport report: reports) {
                String fileID = UUID.randomUUID().toString();
                fileController.saveReport(report.getReport(), fileID);
                helper.addAttachment(report.getReportFileName(), fileController.getReport(fileID));
                sb.append(report.toString()).append("\n\n");
            }
            helper.setText(sb.toString());
            mailSender.send(message);
            logger.info("Email sent to ["+to+"] for ["+inputMessage.getSubject()+"]");
        } catch (MessagingException e) {
            logger.error("Failed to send email message", e);
            throw e;
        }
        if (idsToDelete != null) {
            idsToDelete.parallelStream().forEach(id -> fileController.deleteReport(id));
        }
    }

    boolean checkFileType(InputStream is) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(is);
        return config.getAcceptedMimeTypes().contains(type);
    }

}
