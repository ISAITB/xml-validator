package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    public static final String ARTIFACT_TYPE_SCHEMA = "schema";
    public static final String ARTIFACT_TYPE_SCHEMATRON = "schematron";

    private String mailFrom;
    private boolean mailAuthEnable = true;
    private String mailAuthUsername;
    private String mailAuthPassword;
    private String mailOutboundHost;
    private int mailOutboundPort;
    private boolean mailOutboundSSLEnable = true;
    private String mailInboundHost;
    private int mailInboundPort;
    private boolean mailInboundSSLEnable = true;
    private String mailInboundFolder;
    private boolean includeTestDefinition;

    /**
     * @return True if the domain defines a validation type supporting or requiring user-provided XSDs.
     */
    public boolean definesTypeWithExternalXsd() {
        for (TypedValidationArtifactInfo info : getArtifactInfo().values()) {
            if (info.get(ARTIFACT_TYPE_SCHEMA).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if the domain defines a validation type supporting or requiring user-provided Schematron files.
     */
    public boolean definesTypeWithExternalSchematrons() {
        for (TypedValidationArtifactInfo info : getArtifactInfo().values()) {
            if (info.get(ARTIFACT_TYPE_SCHEMATRON).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get the configuration information for XSDs for the provided validation type.
     *
     * @param validationType The validation type.
     * @return The configuration information.
     */
    public ValidationArtifactInfo getSchemaInfo(String validationType) {
        return getArtifactInfo().get(validationType).get(ARTIFACT_TYPE_SCHEMA);
    }

    /**
     * Get the configuration information for Schematron files for the provided validation type.
     *
     * @param validationType The validation type.
     * @return The configuration information.
     */
    public ValidationArtifactInfo getSchematronInfo(String validationType) {
        return getArtifactInfo().get(validationType).get(ARTIFACT_TYPE_SCHEMATRON);
    }

    /**
     * @return True if tests should be included in validation report items.
     */
    public boolean isIncludeTestDefinition() {
        return includeTestDefinition;
    }

    /**
     * @param includeTestDefinition True if tests should be included in validation report items.
     */
    public void setIncludeTestDefinition(boolean includeTestDefinition) {
        this.includeTestDefinition = includeTestDefinition;
    }

    /**
     * @return True if the domain defines multiple validation types.
     */
    @Override
    public boolean hasMultipleValidationTypes() {
        return getType() != null && getType().size() > 1;
    }

    /**
     * @return The FROM address for sent email replies.
     */
	public String getMailFrom() {
        return mailFrom;
    }

    /**
     * @param mailFrom The FROM address for sent email replies.
     */
    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    /**
     * @return True if authentication is needed for the configured SMTP server.
     */
    public boolean isMailAuthEnable() {
        return mailAuthEnable;
    }

    /**
     * @param mailAuthEnable True if authentication is needed for the configured SMTP server.
     */
    public void setMailAuthEnable(boolean mailAuthEnable) {
        this.mailAuthEnable = mailAuthEnable;
    }

    /**
     * @return The username to use for SMTP authentication.
     */
    public String getMailAuthUsername() {
        return mailAuthUsername;
    }

    /**
     * @param mailAuthUsername The username to use for SMTP authentication.
     */
    public void setMailAuthUsername(String mailAuthUsername) {
        this.mailAuthUsername = mailAuthUsername;
    }

    /**
     * @return The password to use for SMTP authentication.
     */
    public String getMailAuthPassword() {
        return mailAuthPassword;
    }

    /**
     * @param mailAuthPassword The password to use for SMTP authentication.
     */
    public void setMailAuthPassword(String mailAuthPassword) {
        this.mailAuthPassword = mailAuthPassword;
    }

    /**
     * @return The SMTP host outbound messages.
     */
    public String getMailOutboundHost() {
        return mailOutboundHost;
    }

    /**
     * @param mailOutboundHost The SMTP host outbound messages.
     */
    public void setMailOutboundHost(String mailOutboundHost) {
        this.mailOutboundHost = mailOutboundHost;
    }

    /**
     * @return The SMTP port for outbound messages.
     */
    public int getMailOutboundPort() {
        return mailOutboundPort;
    }

    /**
     * @param mailOutboundPort The SMTP port outbound messages.
     */
    public void setMailOutboundPort(int mailOutboundPort) {
        this.mailOutboundPort = mailOutboundPort;
    }

    /**
     * @return True if output SMTP communications are over SSL.
     */
    public boolean isMailOutboundSSLEnable() {
        return mailOutboundSSLEnable;
    }

    /**
     * @param mailOutboundSSLEnable True if output SMTP communications are over SSL.
     */
    public void setMailOutboundSSLEnable(boolean mailOutboundSSLEnable) {
        this.mailOutboundSSLEnable = mailOutboundSSLEnable;
    }

    /**
     * @return The IMAP host for inbound messages.
     */
    public String getMailInboundHost() {
        return mailInboundHost;
    }

    /**
     * @param mailInboundHost The IMAP host for inbound messages.
     */
    public void setMailInboundHost(String mailInboundHost) {
        this.mailInboundHost = mailInboundHost;
    }

    /**
     * @return The IMAP port for inbound messages.
     */
    public int getMailInboundPort() {
        return mailInboundPort;
    }

    /**
     * @param mailInboundPort The IMAP port for inbound messages.
     */
    public void setMailInboundPort(int mailInboundPort) {
        this.mailInboundPort = mailInboundPort;
    }

    /**
     * @return True if inbound IMAP exchanges are over SSL.
     */
    public boolean isMailInboundSSLEnable() {
        return mailInboundSSLEnable;
    }

    /**
     * @param mailInboundSSLEnable True if inbound IMAP exchanges are over SSL.
     */
    public void setMailInboundSSLEnable(boolean mailInboundSSLEnable) {
        this.mailInboundSSLEnable = mailInboundSSLEnable;
    }

    /**
     * @return The name of the IMAP folder to read inbound emails from.
     */
    public String getMailInboundFolder() {
        return mailInboundFolder;
    }

    /**
     * @param mailInboundFolder The name of the IMAP folder to read inbound emails from.
     */
    public void setMailInboundFolder(String mailInboundFolder) {
        this.mailInboundFolder = mailInboundFolder;
    }
}
