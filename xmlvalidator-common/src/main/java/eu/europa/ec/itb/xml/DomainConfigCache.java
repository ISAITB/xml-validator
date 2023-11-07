package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.ParseUtils;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import jakarta.annotation.PostConstruct;
import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static eu.europa.ec.itb.validation.commons.config.ParseUtils.addMissingDefaultValues;

/**
 * Component to load, record and share the domain configurations.
 */
@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    @Autowired
    private ApplicationConfig appConfig = null;

    /**
     * Initialise the configuration.
     */
    @Override
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
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.EMAIL, ValidatorChannel.REST_API};
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#getSupportedChannels()
     *
     * @return Form and SOAP API (email is by default disabled).
     */
    @Override
    protected ValidatorChannel[] getDefaultChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.REST_API};
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
        domainConfig.setIncludeAssertionID(config.getBoolean("validator.includeAssertionID", true));
        // Context files - START
        domainConfig.setContextFileDefaultConfig(ParseUtils.parseValueList("validator.defaultContextFile", config, getContextFileMapper(domainConfig, true)));
        domainConfig.setContextFiles(parseTypeSpecificContextFiles("validator.contextFile", domainConfig.getType(), config, domainConfig));
        // Context files - END
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

    /**
     * Parse a map of validation type to list of objects using a helper function.
     *
     * @param key The common property key.
     * @param types The validation types.
     * @param config The configuration properties.
     * @param domainConfig The currently parsed domain configuration.
     * @return The map.
     */
    public Map<String, List<ContextFileConfig>> parseTypeSpecificContextFiles(String key, List<String> types, Configuration config, DomainConfig domainConfig) {
        Map<String, List<ContextFileConfig>> configValues = new HashMap<>();
        for (String type: types) {
            configValues.put(type, ParseUtils.parseValueList(key + "." + type, config, getContextFileMapper(domainConfig, false)));
        }
        return configValues;
    }

    /**
     * Get the mapping function with which to populate a configuration entry for a context file.
     *
     * @param domainConfig The domain configuration.
     * @param isDefaultConfig Whether the context file is a default one.
     * @return The mapping function.
     */
    private Function<Map<String, String>, ContextFileConfig> getContextFileMapper(DomainConfig domainConfig, boolean isDefaultConfig) {
        Path domainRootPath = Paths.get(appConfig.getResourceRoot(), domainConfig.getDomain());
        final Counter counter = new Counter();
        return (Map<String, String> values) -> {
            Path contextFilePath;
            Optional<Path> schemaPath;
            if (!values.containsKey("path")) {
                throw new IllegalStateException("The 'path' property is mandatory for configured context files.");
            } else {
                var path = Path.of(values.get("path")).normalize();
                if (path.isAbsolute()) {
                    throw new IllegalStateException("The 'path' property for configured context files cannot be an absolute path. Offending path was [%s]".formatted(path.toString()));
                } else {
                    contextFilePath = path;
                }
            }
            if (values.containsKey("schema")) {
                var resolvedSchema = domainRootPath.resolve(Path.of(values.get("schema")).normalize());
                if (Files.notExists(resolvedSchema)) {
                    throw new IllegalStateException("The 'schema' property for configured context files must point to an existing file. Offending path was [%s]".formatted(resolvedSchema.toString()));
                } else {
                    schemaPath = Optional.of(resolvedSchema);
                }
            } else {
                schemaPath = Optional.empty();
            }
            return new ContextFileConfig(contextFilePath, schemaPath, values.containsKey("label"), values.containsKey("placeholder"), counter.getAndIncrement(), isDefaultConfig);
        };
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

    /**
     * Utility class to implement a persistent counter object.
     */
    private static class Counter {
        int counter = 0;

        /**
         * @return The next counter value.
         */
        int getAndIncrement() {
            int valueToReturn = counter;
            counter += 1;
            return valueToReturn;
        }
    }

}
