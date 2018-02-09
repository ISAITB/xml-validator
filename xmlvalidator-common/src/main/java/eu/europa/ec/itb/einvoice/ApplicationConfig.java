package eu.europa.ec.itb.einvoice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig {

    private boolean standalone = false;
    private String resourceRoot;
    private Map<String,String> schematronFile;
    private Map<String,String> schemaFile;
    private File reportFolder;
    private String inputFilePrefix = "ITB-";
    private long minimumCachedInputFileAge = 600000L;
    private long minimumCachedReportFileAge = 600000L;
    private String reportFilePrefix = "TAR-";
    private Set<String> acceptedMimeTypes;
    private Set<String> acceptedSchematronExtensions;
    private String mailFrom;
    private boolean mailAuthEnable = true;
    private String mailAuthUsername = "validate.invoice@gmail.com";
    private String mailAuthPassword = "Admin12345_";
    private String mailOutboundHost = "smtp.gmail.com";
    private int mailOutboundPort = 465;
    private boolean mailOutboundSSLEnable = true;
    private String mailInboundHost = "imap.gmail.com";
    private int mailInboundPort = 993;
    private boolean mailInboundSSLEnable = true;
    private String mailInboundFolder = "INBOX";
    private String uploadTitle = "Validator";
    private String webServiceId = "UBLValidationService";
    private Map<String, String> webServiceDescription;
    private List<String> type;
    private Map<String, String> typeLabel;

    public boolean hasMultipleValidationTypes() {
        return type != null && type.size() > 1;
    }

    public Map<String, String> getSchematronFile() {
        return schematronFile;
    }

    public void setSchematronFile(Map<String, String> schematronFile) {
        this.schematronFile = schematronFile;
    }

    public Map<String, String> getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(Map<String, String> schemaFile) {
        this.schemaFile = schemaFile;
    }

    public File getReportFolder() {
        return reportFolder;
    }

    public void setReportFolder(File reportFolder) {
        this.reportFolder = reportFolder;
    }

    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    public void setInputFilePrefix(String inputFilePrefix) {
        this.inputFilePrefix = inputFilePrefix;
    }

    public long getMinimumCachedInputFileAge() {
        return minimumCachedInputFileAge;
    }

    public void setMinimumCachedInputFileAge(long minimumCachedInputFileAge) {
        this.minimumCachedInputFileAge = minimumCachedInputFileAge;
    }

    public long getMinimumCachedReportFileAge() {
        return minimumCachedReportFileAge;
    }

    public void setMinimumCachedReportFileAge(long minimumCachedReportFileAge) {
        this.minimumCachedReportFileAge = minimumCachedReportFileAge;
    }

    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    public void setReportFilePrefix(String reportFilePrefix) {
        this.reportFilePrefix = reportFilePrefix;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public boolean isMailAuthEnable() {
        return mailAuthEnable;
    }

    public void setMailAuthEnable(boolean mailAuthEnable) {
        this.mailAuthEnable = mailAuthEnable;
    }

    public String getMailAuthUsername() {
        return mailAuthUsername;
    }

    public void setMailAuthUsername(String mailAuthUsername) {
        this.mailAuthUsername = mailAuthUsername;
    }

    public String getMailAuthPassword() {
        return mailAuthPassword;
    }

    public void setMailAuthPassword(String mailAuthPassword) {
        this.mailAuthPassword = mailAuthPassword;
    }

    public String getMailOutboundHost() {
        return mailOutboundHost;
    }

    public void setMailOutboundHost(String mailOutboundHost) {
        this.mailOutboundHost = mailOutboundHost;
    }

    public int getMailOutboundPort() {
        return mailOutboundPort;
    }

    public void setMailOutboundPort(int mailOutboundPort) {
        this.mailOutboundPort = mailOutboundPort;
    }

    public boolean isMailOutboundSSLEnable() {
        return mailOutboundSSLEnable;
    }

    public void setMailOutboundSSLEnable(boolean mailOutboundSSLEnable) {
        this.mailOutboundSSLEnable = mailOutboundSSLEnable;
    }

    public String getMailInboundHost() {
        return mailInboundHost;
    }

    public void setMailInboundHost(String mailInboundHost) {
        this.mailInboundHost = mailInboundHost;
    }

    public int getMailInboundPort() {
        return mailInboundPort;
    }

    public void setMailInboundPort(int mailInboundPort) {
        this.mailInboundPort = mailInboundPort;
    }

    public boolean isMailInboundSSLEnable() {
        return mailInboundSSLEnable;
    }

    public void setMailInboundSSLEnable(boolean mailInboundSSLEnable) {
        this.mailInboundSSLEnable = mailInboundSSLEnable;
    }

    public String getMailInboundFolder() {
        return mailInboundFolder;
    }

    public void setMailInboundFolder(String mailInboundFolder) {
        this.mailInboundFolder = mailInboundFolder;
    }

    public String getUploadTitle() {
        return uploadTitle;
    }

    public void setUploadTitle(String uploadTitle) {
        this.uploadTitle = uploadTitle;
    }

    public String getWebServiceId() {
        return webServiceId;
    }

    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    public Map<String, String> getWebServiceDescription() {
        return webServiceDescription;
    }

    public void setWebServiceDescription(Map<String, String> webServiceDescription) {
        this.webServiceDescription = webServiceDescription;
    }

    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    public List<String> getType() {
        return type;
    }

    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    public Set<String> getAcceptedSchematronExtensions() {
        return acceptedSchematronExtensions;
    }

    public void setAcceptedSchematronExtensions(Set<String> acceptedSchematronExtensions) {
        this.acceptedSchematronExtensions = acceptedSchematronExtensions;
    }
}
