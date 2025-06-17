package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    public static final String ARTIFACT_TYPE_SCHEMA = "schema";
    public static final String ARTIFACT_TYPE_SCHEMATRON = "schematron";
    public static final String COMBINATION_PLACEHOLDER_INPUT = "input";

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
    private boolean includeAssertionID;
    private boolean includeLocationPath = false;
    private List<ContextFileConfig> contextFileConfigDefaultConfig;
    private Map<String, List<ContextFileConfig>> contextFileMap;
    private ContextFileCombinationTemplateConfig contextFileCombinationDefaultTemplate;
    private Map<String, ContextFileCombinationTemplateConfig> contextFileCombinationTemplateMap;
    private Map<String, Path> inputTransformerMap;
    private Map<String, Boolean> stopOnXsdErrors;

    /** @return The map of full validation types to XSLT files for input transformation. */
    public Map<String, Path> getInputTransformerMap() {
        return inputTransformerMap;
    }

    /** @param inputTransformerMap The map of full validation types to XSLT files for input transformation. */
    public void setInputTransformerMap(Map<String, Path> inputTransformerMap) {
        this.inputTransformerMap = inputTransformerMap;
    }

    /**
     * Get the context file combination template to apply for the given validation type.
     *
     * @param validationType The validation type.
     * @return The template file to use.
     */
    public Optional<ContextFileCombinationTemplateConfig> getContextFileCombinationTemplate(String validationType) {
        var template = getContextFileCombinationTemplateMap().get(validationType);
        if (template == null) {
            template = getContextFileCombinationDefaultTemplate();
        }
        return Optional.ofNullable(template);
    }

    /** @return The default template file to use for combining context files with input files (null if not applicable). */
    public ContextFileCombinationTemplateConfig getContextFileCombinationDefaultTemplate() {
        return contextFileCombinationDefaultTemplate;
    }

    /** @param contextFileCombinationDefaultTemplate The default template file to use for combining context files with input files (null if not applicable). */
    public void setContextFileCombinationDefaultTemplate(ContextFileCombinationTemplateConfig contextFileCombinationDefaultTemplate) {
        this.contextFileCombinationDefaultTemplate = contextFileCombinationDefaultTemplate;
    }

    /** @return The map of full validation types to context file combination template file. */
    public Map<String, ContextFileCombinationTemplateConfig> getContextFileCombinationTemplateMap() {
        return contextFileCombinationTemplateMap;
    }

    /** @param contextFileCombinationTemplateMap The map of full validation types to context file combination template file. */
    public void setContextFileCombinationTemplateMap(Map<String, ContextFileCombinationTemplateConfig> contextFileCombinationTemplateMap) {
        this.contextFileCombinationTemplateMap = contextFileCombinationTemplateMap;
    }

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
     * @param stopOnXsdErrors Set the map of full validation types to whether validation should stop on XSD errors.
     */
    public void setStopOnXsdErrors(Map<String, Boolean> stopOnXsdErrors) {
        this.stopOnXsdErrors = stopOnXsdErrors;
    }

    /**
     * Check to see whether validation should stop if we have an XSD error. This prevents Schematron rules to be evaluated
     * in case of XSD errors.
     *
     * @param validationType The full validation type.
     * @return Whether we should stop validation on XSD error (default is true).
     */
    public boolean stopOnXsdErrors(String validationType) {
        return this.stopOnXsdErrors == null || this.stopOnXsdErrors.getOrDefault(validationType, Boolean.TRUE);
    }

    /**
     * Get the context files that apply for the given validation type.
     *
     * @param validationType The validation type.
     * @return The context files.
     */
    public List<ContextFileConfig> getContextFiles(String validationType) {
        var configs = contextFileConfigDefaultConfig;
        if (contextFileMap.containsKey(validationType)) {
            var specificConfigs = contextFileMap.get(validationType);
            if (specificConfigs != null && !specificConfigs.isEmpty()) {
                configs = specificConfigs;
            }
        }
        return configs;
    }

    /**
     * Check to see whether any validation type expects context files.
     *
     * @return The check result.
     */
    public boolean hasContextFiles() {
        return (contextFileConfigDefaultConfig != null && !contextFileConfigDefaultConfig.isEmpty()) ||
               (contextFileMap != null && contextFileMap.values().stream().anyMatch(configs -> configs != null && !configs.isEmpty()));
    }

    /**
     * Set the default context files for all validation types.
     *
     * @param contextFileConfigDefaultConfig The context files.
     */
    public void setContextFileDefaultConfig(List<ContextFileConfig> contextFileConfigDefaultConfig) {
        this.contextFileConfigDefaultConfig = contextFileConfigDefaultConfig;
    }

    /**
     * Set the context files for the given validation type.
     *
     * @param contextFiles The context files.
     */
    public void setContextFiles(Map<String, List<ContextFileConfig>> contextFiles) {
        this.contextFileMap = contextFiles;
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
     * @return True if path locations should be included in validation report items.
     */
    public boolean isIncludeLocationPath() {
        return includeLocationPath;
    }

    /**
     * @param includeLocationPath True if path locations should be included in validation report items.
     */
    public void setIncludeLocationPath(boolean includeLocationPath) {
        this.includeLocationPath = includeLocationPath;
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
     * @return True if assertion IDs should be included in validation report items.
     */
    public boolean isIncludeAssertionID() {
        return includeAssertionID;
    }

    /**
     * @param includeAssertionID True if assertion IDs should be included in validation report items.
     */
    public void setIncludeAssertionID(boolean includeAssertionID) {
        this.includeAssertionID = includeAssertionID;
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
