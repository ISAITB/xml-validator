package eu.europa.ec.itb.xml;

import java.nio.file.Path;

/**
 * Record capturing the configuration for context file combination templates.
 *
 * @param path The resolved path for the template.
 * @param configuredPath The path as a string, as configured for the specific validator.
 */
public record ContextFileCombinationTemplateConfig(Path path, String configuredPath) {
}
