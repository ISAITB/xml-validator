package eu.europa.ec.itb.einvoice;

import org.apache.commons.configuration.*;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by simatosc on 04/03/2016.
 */
@Component
public class Configuration {

    private static Configuration instance;

    private File schematronFolder;
    private File schemaFile;
    private File reportFolder;
    private String inputFilePrefix;
    private long minimumCachedInputFileAge;
    private long minimumCachedReportFileAge;
    private String reportFilePrefix;
    private Set<String> acceptedMimeTypes;
    private String mailFrom;
    private boolean mailAuthEnable;
    private String mailAuthUsername;
    private String mailAuthPassword;
    private String mailOutboundHost;
    private int mailOutboundPort;
    private boolean mailOutboundSSLEnable;
    private String mailInboundHost;
    private int mailInboundPort;
    private boolean mailInboundSSLEnable;
    private String mailInboundFolder;

    public File getSchematronFolder() {
        return schematronFolder;
    }

    public File getSchemaFile() {
        return schemaFile;
    }

    public File getReportFolder() {
        return reportFolder;
    }

    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    public long getMinimumCachedInputFileAge() {
        return minimumCachedInputFileAge;
    }

    public long getMinimumCachedReportFileAge() {
        return minimumCachedReportFileAge;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public boolean isMailAuthEnable() {
        return mailAuthEnable;
    }

    public String getMailAuthUsername() {
        return mailAuthUsername;
    }

    public String getMailAuthPassword() {
        return mailAuthPassword;
    }

    public String getMailOutboundHost() {
        return mailOutboundHost;
    }

    public int getMailOutboundPort() {
        return mailOutboundPort;
    }

    public boolean isMailOutboundSSLEnable() {
        return mailOutboundSSLEnable;
    }

    public String getMailInboundHost() {
        return mailInboundHost;
    }

    public int getMailInboundPort() {
        return mailInboundPort;
    }

    public boolean isMailInboundSSLEnable() {
        return mailInboundSSLEnable;
    }

    public String getMailInboundFolder() {
        return mailInboundFolder;
    }

    @Bean
    public Configuration config() {
        return new Configuration();
    }

    @PostConstruct
    public void initialize() {
        CompositeConfiguration config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());
        config.addConfiguration(new EnvironmentConfiguration());
        String configPath = config.getString("config.path", "config.properties");
        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setListDelimiter(',');
        props.setFileName(configPath);
        try {
            props.load();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Error loading configuration property file from ["+configPath+"]", e);
        }
        config.addConfiguration(props);
        schematronFolder = new File(config.getString("path.sch.folder"));
        if (!schematronFolder.exists() || !schematronFolder.isDirectory()) {
            throw new IllegalStateException("Schematron source folder ["+schematronFolder.getAbsolutePath()+"] is not a valid directory.");
        }
        schemaFile = new File(config.getString("path.xsd.file"));
        if (!schemaFile.exists() || !schemaFile.isFile()) {
            throw new IllegalStateException("Schema file ["+schemaFile.getAbsolutePath()+"] is not valid.");
        }
        reportFolder = new File(config.getString("path.report.folder", "./reports"));
        if (reportFolder.exists() && reportFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(reportFolder);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to clean up report folder", e);
            }
        }
        reportFolder.mkdir();
        minimumCachedInputFileAge = config.getLong("inputFile.minimumCacheTime", 600000L);
        minimumCachedReportFileAge = config.getLong("reportFile.minimumCacheTime", 600000L);
        inputFilePrefix = config.getString("inputFile.prefix", "ITB-");
        reportFilePrefix = config.getString("reportFile.prefix", "TAR-");
        acceptedMimeTypes = new HashSet<>(Arrays.asList(config.getStringArray("inputFile.acceptedMimeType")));

        mailFrom = config.getString("mail.from", "UBL Invoice Validator <validate.invoice@gmail.com>");
        mailAuthEnable = config.getBoolean("mail.auth.enable", true);
        mailAuthUsername = config.getString("mail.auth.username", "validate.invoice@gmail.com");
        mailAuthPassword = config.getString("mail.auth.password", "Admin12345_");
        mailOutboundHost = config.getString("mail.outbound.host", "smtp.gmail.com");
        mailOutboundPort = config.getInt("mail.outbound.port", 465);
        mailOutboundSSLEnable = config.getBoolean("mail.outbound.ssl.enable", true);
        mailInboundHost = config.getString("mail.inbound.host", "imap.gmail.com");
        mailInboundPort = config.getInt("mail.inbound.port", 993);
        mailInboundSSLEnable = config.getBoolean("mail.inbound.ssl.enable", true);
        mailInboundFolder = config.getString("mail.inbound.folder", "INBOX");
    }

}
