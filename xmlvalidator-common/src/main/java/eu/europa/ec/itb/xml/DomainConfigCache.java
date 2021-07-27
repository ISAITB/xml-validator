package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.configuration2.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Component to load, record and share the domain configurations.
 */
@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    /**
     * Initialise the configuration.
     */
    @PostConstruct
    public void init() {
        super.init();
    }

    /**
     * Create a new and empty domain configuration object.
     *
     * @return The object.
     */
    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.EMAIL};
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#getSupportedChannels()
     *
     * @return Form and SOAP API (email is by default disabled).
     */
    @Override
    protected ValidatorChannel[] getDefaultChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API};
    }

    /**
     * Extend the domain configuration loading with XML-specific information.
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
    @Override
    protected void addDomainConfiguration(DomainConfig domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        addValidationArtifactInfoForType("schema", "validator.schemaFile", "validator.externalSchemaFile", null, domainConfig, config);
        addValidationArtifactInfoForType("schematron", "validator.schematronFile", "validator.externalSchematronFile", null, domainConfig, config);
        domainConfig.setMailFrom(config.getString("validator.mailFrom", null));
        domainConfig.setMailAuthEnable(config.getBoolean("validator.mailAuthEnable", false));
        domainConfig.setMailAuthUsername(config.getString("validator.mailAuthUsername", null));
        domainConfig.setMailAuthPassword(config.getString("validator.mailAuthPassword", null));
        domainConfig.setMailOutboundHost(config.getString("validator.mailOutboundHost", null));
        domainConfig.setMailOutboundPort(config.getInt("validator.mailOutboundPort", -1));
        domainConfig.setMailOutboundSSLEnable(config.getBoolean("validator.mailOutboundSSLEnable", false));
        domainConfig.setMailInboundHost(config.getString("validator.mailInboundHost", null));
        domainConfig.setMailInboundPort(config.getInt("validator.mailInboundPort", -1));
        domainConfig.setMailInboundSSLEnable(config.getBoolean("validator.mailInboundSSLEnable", false));
        domainConfig.setMailInboundFolder(config.getString("validator.mailInboundFolder", "INBOX"));
        domainConfig.setIncludeTestDefinition(config.getBoolean("validator.includeTestDefinition", true));
        domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
        // Labels
        domainConfig.getLabel().setPopupTitle(config.getString("validator.label.popupTitle", "XML content"));
        domainConfig.getLabel().setExternalSchemaLabel(config.getString("validator.label.externalSchemaLabel", "XML Schema"));
        domainConfig.getLabel().setExternalSchematronLabel(config.getString("validator.label.externalSchematronLabel", "Schematron"));
        domainConfig.getLabel().setExternalSchemaPlaceholder(config.getString("validator.label.externalSchemaPlaceholder", ""));
        domainConfig.getLabel().setExternalSchematronPlaceholder(config.getString("validator.label.externalSchematronPlaceholder", ""));
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#toValidatorChannel(Set, String)
     *
     * For backwards compatibility with old configurations, considers "webservice" equivalent to "soap_api".
     *
     * @param supportedChannels The supported channels.
     * @param channelName The channel name to resolve.
     * @return The channel.
     */
    @Override
    protected ValidatorChannel toValidatorChannel(Set<ValidatorChannel> supportedChannels, String channelName) {
        if ("webservice".equals(channelName)) {
            // For backwards compatibility (webservice = soap_api).
            channelName = ValidatorChannel.SOAP_API.getName();
        }
        return super.toValidatorChannel(supportedChannels, channelName);
    }

}
