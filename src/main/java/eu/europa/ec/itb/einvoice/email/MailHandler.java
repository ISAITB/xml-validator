package eu.europa.ec.itb.einvoice.email;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.Configuration;
import eu.europa.ec.itb.einvoice.upload.FileController;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by simatosc on 16/03/2016.
 */
@Component
public class MailHandler {

    private static Logger logger = LoggerFactory.getLogger(MailHandler.class);

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    FileController fileController;

    @Autowired
    Configuration config;

    @Autowired
    BeanFactory beans;

    @Scheduled(fixedDelayString = "${mail.polling.rate}")
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
                                logger.info("Processing file ["+part.getFileName()+"] of ["+message.getSubject()+"]");
                                try (InputStream is = part.getInputStream()) {
                                    XMLValidator validator = beans.getBean(XMLValidator.class, is);
                                    TAR report = validator.validateAll();
                                    reports.add(new FileReport(part.getFileName(), report));
                                    logger.info("Processed message ["+message.getSubject()+"], file ["+part.getFileName()+"]");
                                }
                            } else {
                                logger.info("Ignoring file ["+part.getFileName()+"] of ["+message.getSubject()+"]");
                            }
                        }
                        if (reports.isEmpty()) {
                            logger.info("No reports to send for ["+message.getSubject()+"]");
                        } else {
                            logger.info("Sending email response for ["+message.getSubject()+"]");
                            sendEmail(message, reports);
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                    } else {
                        logger.info("Skipping message ["+message.getSubject()+"] as non-multipart");
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

    public void sendEmail(Message inputMessage, Collection<FileReport> reports) {
        MimeMessage message = mailSender.createMimeMessage();
        List<String> idsToDelete = new ArrayList<>();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String to = inputMessage.getFrom()[0].toString();
            helper.setTo(to);
            helper.setFrom(config.getMailFrom());
            helper.setSubject("Validation report ["+inputMessage.getSubject()+"]");
            StringBuilder sb = new StringBuilder();
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

    private class FileReport {

        private final String fileName;
        private final TAR report;

        private FileReport(String fileName, TAR report) {
            this.fileName = fileName;
            this.report = report;
        }

        String getFileName() {
            return fileName;
        }

        TAR getReport() {
            return report;
        }

        String getReportFileName() {
            return fileName.substring(0, fileName.lastIndexOf('.'))+".report.xml";
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation report for [").append(fileName).append("]:");
            sb.append("\n- Date: ").append(report.getDate());
            sb.append("\n- Result: ").append(report.getResult());
            sb.append("\n- Errors: ").append(report.getCounters().getNrOfErrors());
            sb.append("\n- Warnings: ").append(report.getCounters().getNrOfWarnings());
            sb.append("\n- Messages: ").append(report.getCounters().getNrOfAssertions());
            sb.append("\n- Detailed report attached in: [").append(getReportFileName()).append(']');
            return sb.toString();
        }
    }
}
