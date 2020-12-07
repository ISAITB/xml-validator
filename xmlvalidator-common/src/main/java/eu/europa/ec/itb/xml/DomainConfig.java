package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

/**
 * Created by simatosc on 21/03/2016.
 */
public class DomainConfig extends WebDomainConfig<DomainConfig.LabelConfig> {

    public static final String ARTIFACT_TYPE_SCHEMA = "schema";
    public static final String ARTIFACT_TYPE_SCHEMATRON = "schematron";

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
    private boolean includeTestDefinition;
    private boolean reportsOrdered;

    public boolean definesTypeWithExternalXsd() {
        for (TypedValidationArtifactInfo info : getArtifactInfo().values()) {
            if (info.get(ARTIFACT_TYPE_SCHEMA).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    public boolean definesTypeWithExternalSchematrons() {
        for (TypedValidationArtifactInfo info : getArtifactInfo().values()) {
            if (info.get(ARTIFACT_TYPE_SCHEMATRON).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected LabelConfig newLabelConfig() {
        return new LabelConfig();
    }

    public ValidationArtifactInfo getSchemaInfo(String validationType) {
        return getArtifactInfo().get(validationType).get(ARTIFACT_TYPE_SCHEMA);
    }

    public ValidationArtifactInfo getSchematronInfo(String validationType) {
        return getArtifactInfo().get(validationType).get(ARTIFACT_TYPE_SCHEMATRON);
    }

    public boolean isIncludeTestDefinition() {
        return includeTestDefinition;
    }

    public void setIncludeTestDefinition(boolean includeTestDefinition) {
        this.includeTestDefinition = includeTestDefinition;
    }

    public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
    }

    public boolean hasMultipleValidationTypes() {
        return getType() != null && getType().size() > 1;
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

	public static class LabelConfig extends eu.europa.ec.itb.validation.commons.config.LabelConfig {

        private String externalSchemaLabel;
        private String externalSchematronLabel;
        private String externalSchemaPlaceholder;
        private String externalSchematronPlaceholder;

		public String getExternalSchemaLabel() {
			return externalSchemaLabel;
		}

		public void setExternalSchemaLabel(String externalSchemaLabel) {
			this.externalSchemaLabel = externalSchemaLabel;
		}

		public String getExternalSchematronLabel() {
			return externalSchematronLabel;
		}

		public void setExternalSchematronLabel(String externalSchematronLabel) {
			this.externalSchematronLabel = externalSchematronLabel;
		}

        public String getExternalSchemaPlaceholder() {
            return externalSchemaPlaceholder;
        }

        public void setExternalSchemaPlaceholder(String externalSchemaPlaceholder) {
            this.externalSchemaPlaceholder = externalSchemaPlaceholder;
        }

        public String getExternalSchematronPlaceholder() {
            return externalSchematronPlaceholder;
        }

        public void setExternalSchematronPlaceholder(String externalSchematronPlaceholder) {
            this.externalSchematronPlaceholder = externalSchematronPlaceholder;
        }
    }

}
