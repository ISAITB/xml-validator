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
import java.util.stream.Collectors;

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
        domainConfig.setIncludeLocationPath(config.getBoolean("validator.includeLocationPath", false));
        // Context files - START
        domainConfig.setContextFileDefaultConfig(ParseUtils.parseValueList("validator.defaultContextFile", config, getContextFileMapper(domainConfig, true)));
        domainConfig.setContextFiles(parseTypeSpecificContextFiles("validator.contextFile", domainConfig.getType(), config, domainConfig));
        parseContextFileCombinationTemplates(config, domainConfig);
        // Context files - END
        // Input transformations - start
        domainConfig.setInputTransformerMap(parseInputFileTransformers(ParseUtils.parseMap("validator.input.transformer", config, domainConfig.getType()), domainConfig));
        // Input transformations - end
        // Stop on XSD errors - start
        domainConfig.setStopOnXsdErrors(ParseUtils.parseBooleanMap("validator.stopOnXsdErrors", config, domainConfig.getType(),
                config.getBoolean("validator.stopOnXsdErrors", true)
        ));
        // Stop on XSD errors - stop
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

    /**
     * Calculate the domain configuration root folder.
     *
     * @param domainConfig The current configuration.
     * @return The path.
     */
    private Path getDomainRootPath(DomainConfig domainConfig) {
        return Paths.get(appConfig.getResourceRoot(), domainConfig.getDomain());
    }

    /**
     * Parse and validate the configured input transformation files as relative paths.
     *
     * @param parsedPaths The configured paths.
     * @param domainConfig The domain configuration.
     * @return The path map.
     */
    private Map<String, Path> parseInputFileTransformers(Map<String, String> parsedPaths, DomainConfig domainConfig) {
        Path domainRootPath = getDomainRootPath(domainConfig);
        return parsedPaths.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            var path = domainRootPath.resolve(entry.getValue()).normalize();
            if (!Files.exists(path) || !path.startsWith(domainRootPath)) {
                throw new IllegalStateException("Input transformation files must exist as files under the domain root folder. Offending path was [%s]".formatted(entry.getValue()));
            } else {
                return path;
            }
        }));
    }

    /**
     * Parse the context file combination templates (per type and default).
     *
     * @param config The configuration properties.
     * @param domainConfig The currently parsed domain configuration.
     */
    private void parseContextFileCombinationTemplates(Configuration config, DomainConfig domainConfig) {
        Map<String, String> pathStringsPerType = ParseUtils.parseMap("validator.contextFileCombinationTemplate", config, domainConfig.getType());
        // Ensure that each configured path is valid and convert it to a Path.
        Map<String, ContextFileCombinationTemplateConfig> pathsPerType = new HashMap<>();
        Path domainRootPath = getDomainRootPath(domainConfig);
        for (var entry: pathStringsPerType.entrySet()) {
            var configuredPath = entry.getValue();
            var resolvedPath = domainRootPath.resolve(Path.of(configuredPath)).normalize();
            if (Files.notExists(resolvedPath)) {
                throw new IllegalStateException("Context file combination template files must point to existing files. Offending path was [%s]".formatted(configuredPath));
            } else if (!resolvedPath.startsWith(domainRootPath)) {
                throw new IllegalStateException("Context file combination template files must be under the domain root folder. Offending path was [%s]".formatted(configuredPath));
            }
            pathsPerType.put(entry.getKey(), new ContextFileCombinationTemplateConfig(resolvedPath, configuredPath));
        }
        domainConfig.setContextFileCombinationTemplateMap(pathsPerType);
        // Parse also the default combination template.
        String defaultPath = config.getString("validator.defaultContextFileCombinationTemplate");
        if (defaultPath != null) {
            var resolvedPath = domainRootPath.resolve(Path.of(defaultPath)).normalize();
            if (Files.notExists(resolvedPath)) {
                throw new IllegalStateException("The default context file combination template must point to an existing file. Offending path was [%s]".formatted(defaultPath));
            } else if (!resolvedPath.startsWith(domainRootPath)) {
                throw new IllegalStateException("The default context file combination template must be under the domain root folder. Offending path was [%s]".formatted(defaultPath));
            }
            domainConfig.setContextFileCombinationDefaultTemplate(new ContextFileCombinationTemplateConfig(resolvedPath, defaultPath));
        }
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
        Path domainRootPath = getDomainRootPath(domainConfig);
        final Counter counter = new Counter();
        return (Map<String, String> values) -> {
            Path contextFilePath;
            Optional<Path> schemaPath;
            String configuredPath;
            if (!values.containsKey("path")) {
                throw new IllegalStateException("The 'path' property is mandatory for configured context files.");
            } else {
                configuredPath = values.get("path");
                var path = Path.of(configuredPath);
                if (path.isAbsolute()) {
                    throw new IllegalStateException("The 'path' property for configured context files cannot be an absolute path. Offending path was [%s]".formatted(path.toString()));
                } else if (!domainRootPath.resolve(path).normalize().startsWith(domainRootPath)) {
                    throw new IllegalStateException("The 'path' property for configured context files cannot resolve to a path outside the domain root folder. Offending path was [%s]".formatted(path.toString()));
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
            Optional<String> combinationPlaceholder = Optional.empty();
            if (values.containsKey("combinationPlaceholder")) {
                var placeholder = values.get("combinationPlaceholder");
                if (DomainConfig.COMBINATION_PLACEHOLDER_INPUT.equals(placeholder)) {
                    throw new IllegalStateException("Placeholder '%s' is reserved for the main validation input. Please select another placeholder value for context file [%s].".formatted(DomainConfig.COMBINATION_PLACEHOLDER_INPUT, configuredPath));
                }
                combinationPlaceholder = Optional.of(placeholder);
            }
            return new ContextFileConfig(contextFilePath, configuredPath, schemaPath, values.containsKey("label"), values.containsKey("placeholder"), combinationPlaceholder, counter.getAndIncrement(), isDefaultConfig);
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
