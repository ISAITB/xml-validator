package eu.europa.ec.itb.einvoice;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 */
public class DomainConfig {

    private boolean isDefined;
    private String domain;
    private String domainName;
    private String uploadTitle = "Validator";
    private String webServiceId = "UBLValidationService";
    private String reportTitle = "Validation report";
    private Map<String, String> webServiceDescription;
    private List<String> type;
    private Set<ValidatorChannel> channels;
    private Map<String, String> typeLabel;
    private Map<String, ValidationArtifactInfo> schematronFile;
    private Map<String, ValidationArtifactInfo> schemaFile;
    private Map<String, ExternalValidationArtifactInfo> externalSchemaFile;
    private Map<String, ExternalValidationArtifactInfo> externalSchematronFile;
    private Map<String, RemoteFileInfo> remoteSchemaFile;
    private Map<String, RemoteFileInfo> remoteSchematronFile;
    private String htmlBanner;
    private String htmlFooter;
    private boolean supportMinimalUserInterface;

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
    private boolean showAbout;

    private boolean includeTestDefinition;
    private boolean reportsOrdered;
    
    public static final String externalFile_req     	= "required" ;
    public static final String externalFile_opt     	= "optional" ;
    public static final String externalFile_none     	= "none" ;

    private Label label = new Label();

    public DomainConfig() {
        this(true);
    }

    public DomainConfig(boolean isDefined) {
        this.isDefined = isDefined;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomainName() {
        return domainName;
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
        return type != null && type.size() > 1;
    }

    public Map<String, ValidationArtifactInfo> getSchematronFile() {
        return schematronFile;
    }

    public void setSchematronFile(Map<String, ValidationArtifactInfo> schematronFile) {
        this.schematronFile = schematronFile;
    }

    public Map<String, ValidationArtifactInfo> getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(Map<String, ValidationArtifactInfo> schemaFile) {
        this.schemaFile = schemaFile;
    }

    public Map<String, ExternalValidationArtifactInfo> getExternalSchemaFile() {
        return externalSchemaFile;
    }

    public void setExternalSchemaFile(Map<String, ExternalValidationArtifactInfo> externalSchemaFile) {
        this.externalSchemaFile = externalSchemaFile;
    }

    public Map<String, ExternalValidationArtifactInfo> getExternalSchematronFile() {
        return externalSchematronFile;
    }

    public void setExternalSchematronFile(Map<String, ExternalValidationArtifactInfo> externalSchematronFile) {
        this.externalSchematronFile = externalSchematronFile;
    }

    public Map<String, RemoteFileInfo> getRemoteSchemaFile() {
        return remoteSchemaFile;
    }

    public void setRemoteSchemaFile(Map<String, RemoteFileInfo> remoteSchemaFile) {
        this.remoteSchemaFile = remoteSchemaFile;
    }

    public Map<String, RemoteFileInfo> getRemoteSchematronFile() {
        return remoteSchematronFile;
    }

    public void setRemoteSchematronFile(Map<String, RemoteFileInfo> remoteSchematronFile) {
        this.remoteSchematronFile = remoteSchematronFile;
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

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    public boolean isShowAbout() {
        return showAbout;
    }

    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getHtmlBanner() {
		return htmlBanner;
	}

	public void setHtmlBanner(String htmlBanner) {
		this.htmlBanner = htmlBanner;
	}

    public String getHtmlFooter() {
		return htmlFooter;
	}

	public void setHtmlFooter(String htmlFooter) {
		this.htmlFooter = htmlFooter;
	}
	
	public boolean isSupportMinimalUserInterface() {
		return supportMinimalUserInterface;
	}

	public void setSupportMinimalUserInterface(boolean supportMinimalUserInterface) {
		this.supportMinimalUserInterface = supportMinimalUserInterface;
	}

	public static class RemoteFileInfo {
    	List<ValidationArtifactInfo> remote;
    	
    	public List<ValidationArtifactInfo> getRemote() {
    		return remote; 
    	}
    	void setRemote(List<ValidationArtifactInfo> remote) {
    		this.remote = remote; 
    	}
    }

    public static class ExternalValidationArtifactInfo {
        private String supportForExternalArtifacts;
        private String preProcessorPath;
        private String preProcessorOutputExtension;

        public String getSupportForExternalArtifacts() {
            return supportForExternalArtifacts;
        }
        void setSupportForExternalArtifacts(String supportForExternalArtifacts) {
            this.supportForExternalArtifacts = supportForExternalArtifacts;
        }
        public String getPreProcessorPath() {
            return preProcessorPath;
        }
        void setPreProcessorPath(String preProcessorPath) {
            this.preProcessorPath = preProcessorPath;
        }
        public String getPreProcessorOutputExtension() {
            return preProcessorOutputExtension;
        }
        void setPreProcessorOutputExtension(String preProcessorOutputExtension) {
            this.preProcessorOutputExtension = preProcessorOutputExtension;
        }
    }

    public static class ValidationArtifactInfo {
        private String path;
        private String preProcessorPath;
        private String preProcessorOutputExtension;

        public String getPath() {
            return path;
        }
        void setPath(String path) {
            this.path = path;
        }
        public String getPreProcessorPath() {
            return preProcessorPath;
        }
        void setPreProcessorPath(String preProcessorPath) {
            this.preProcessorPath = preProcessorPath;
        }
        public String getPreProcessorOutputExtension() {
            return preProcessorOutputExtension;
        }
        void setPreProcessorOutputExtension(String preProcessorOutputExtension) {
            this.preProcessorOutputExtension = preProcessorOutputExtension;
        }
    }

	public static class Label {

        private String resultSectionTitle;
        private String fileInputLabel;
        private String fileInputPlaceholder;
        private String typeLabel;
        private String uploadButton;
        private String resultSubSectionOverviewTitle;
        private String resultDateLabel;
        private String resultFileNameLabel;
        private String resultResultLabel;
        private String resultErrorsLabel;
        private String resultWarningsLabel;
        private String resultMessagesLabel;
        private String viewAnnotatedInputButton;
        private String downloadXMLReportButton;
        private String downloadPDFReportButton;
        private String resultSubSectionDetailsTitle;
        private String resultTestLabel;
        private String popupTitle;
        private String popupCloseButton;
        private String resultValidationTypeLabel;
        private String optionContentFile;
        private String optionContentURI;
        private String optionContentDirectInput;
        private String includeExternalArtefacts;
        private String externalArtefactsTooltip;
        private String externalSchemaLabel;
        private String externalSchematronLabel;
        private String externalSchemaPlaceholder;
        private String externalSchematronPlaceholder;

        public String getResultSectionTitle() {
            return resultSectionTitle;
        }

        public void setResultSectionTitle(String resultSectionTitle) {
            this.resultSectionTitle = resultSectionTitle;
        }

        public String getFileInputLabel() {
            return fileInputLabel;
        }

        public void setFileInputLabel(String fileInputLabel) {
            this.fileInputLabel = fileInputLabel;
        }

        public String getFileInputPlaceholder() {
            return fileInputPlaceholder;
        }

        public void setFileInputPlaceholder(String fileInputPlaceholder) {
            this.fileInputPlaceholder = fileInputPlaceholder;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public String getUploadButton() {
            return uploadButton;
        }

        public void setUploadButton(String uploadButton) {
            this.uploadButton = uploadButton;
        }

        public String getResultSubSectionOverviewTitle() {
            return resultSubSectionOverviewTitle;
        }

        public void setResultSubSectionOverviewTitle(String resultSubSectionOverviewTitle) {
            this.resultSubSectionOverviewTitle = resultSubSectionOverviewTitle;
        }

        public String getResultDateLabel() {
            return resultDateLabel;
        }

        public void setResultDateLabel(String resultDateLabel) {
            this.resultDateLabel = resultDateLabel;
        }

        public String getResultFileNameLabel() {
            return resultFileNameLabel;
        }

        public void setResultFileNameLabel(String resultFileNameLabel) {
            this.resultFileNameLabel = resultFileNameLabel;
        }

        public String getResultResultLabel() {
            return resultResultLabel;
        }

        public void setResultResultLabel(String resultResultLabel) {
            this.resultResultLabel = resultResultLabel;
        }

        public String getResultErrorsLabel() {
            return resultErrorsLabel;
        }

        public void setResultErrorsLabel(String resultErrorsLabel) {
            this.resultErrorsLabel = resultErrorsLabel;
        }

        public String getResultWarningsLabel() {
            return resultWarningsLabel;
        }

        public void setResultWarningsLabel(String resultWarningsLabel) {
            this.resultWarningsLabel = resultWarningsLabel;
        }

        public String getResultMessagesLabel() {
            return resultMessagesLabel;
        }

        public void setResultMessagesLabel(String resultMessagesLabel) {
            this.resultMessagesLabel = resultMessagesLabel;
        }

        public String getViewAnnotatedInputButton() {
            return viewAnnotatedInputButton;
        }

        public void setViewAnnotatedInputButton(String viewAnnotatedInputButton) {
            this.viewAnnotatedInputButton = viewAnnotatedInputButton;
        }

        public String getDownloadXMLReportButton() {
            return downloadXMLReportButton;
        }

        public void setDownloadXMLReportButton(String downloadXMLReportButton) {
            this.downloadXMLReportButton = downloadXMLReportButton;
        }

        public String getDownloadPDFReportButton() {
            return downloadPDFReportButton;
        }

        public void setDownloadPDFReportButton(String downloadPDFReportButton) {
            this.downloadPDFReportButton = downloadPDFReportButton;
        }

        public String getResultSubSectionDetailsTitle() {
            return resultSubSectionDetailsTitle;
        }

        public void setResultSubSectionDetailsTitle(String resultSubSectionDetailsTitle) {
            this.resultSubSectionDetailsTitle = resultSubSectionDetailsTitle;
        }

        public String getResultTestLabel() {
            return resultTestLabel;
        }

        public void setResultTestLabel(String resultTestLabel) {
            this.resultTestLabel = resultTestLabel;
        }

        public String getPopupTitle() {
            return popupTitle;
        }

        public void setPopupTitle(String popupTitle) {
            this.popupTitle = popupTitle;
        }

        public String getPopupCloseButton() {
            return popupCloseButton;
        }

        public void setPopupCloseButton(String popupCloseButton) {
            this.popupCloseButton = popupCloseButton;
        }

        public String getResultValidationTypeLabel() {
            return resultValidationTypeLabel;
        }

        public void setResultValidationTypeLabel(String resultValidationTypeLabel) {
            this.resultValidationTypeLabel = resultValidationTypeLabel;
        }
        
        public String getOptionContentFile() {
            return optionContentFile;
        }

        public void setOptionContentFile(String optionContentFile) {
            this.optionContentFile = optionContentFile;
        }

        public String getOptionContentURI() {
            return optionContentURI;
        }

        public void setOptionContentURI(String optionContentURI) {
            this.optionContentURI = optionContentURI;
        }

        public String getOptionContentDirectInput() {
            return optionContentDirectInput;
        }

        public void setOptionContentDirectInput(String optionContentDirectInput) {
            this.optionContentDirectInput = optionContentDirectInput;
        }

		public String getIncludeExternalArtefacts() {
			return includeExternalArtefacts;
		}

		public void setIncludeExternalArtefacts(String includeExternalArtefacts) {
			this.includeExternalArtefacts = includeExternalArtefacts;
		}

		public String getExternalArtefactsTooltip() {
			return externalArtefactsTooltip;
		}

		public void setExternalArtefactsTooltip(String externalArtefactsTooltip) {
			this.externalArtefactsTooltip = externalArtefactsTooltip;
		}

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
