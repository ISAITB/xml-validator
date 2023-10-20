package eu.europa.ec.itb.xml;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration information for a context file.
 *
 * @param path The path at which to store the context file.
 * @param schema The schema with which to validate the file.
 * @param hasLabel Whether the context file has a specific label defined.
 * @param hasPlaceholder Whether the context file has a specific placeholder text defined.
 * @param index The sequential index of the context file's configuration entry.
 * @param defaultConfig Whether the context file is a default one (as opposed to a per-validation type one).
 */
public record ContextFileConfig(Path path, Optional<Path> schema, boolean hasLabel, boolean hasPlaceholder, int index, boolean defaultConfig) {
}
