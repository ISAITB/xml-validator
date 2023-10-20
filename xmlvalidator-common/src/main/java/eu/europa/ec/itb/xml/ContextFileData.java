package eu.europa.ec.itb.xml;

import java.nio.file.Path;

/**
 * The information to use relative to a context file for the validation.
 *
 * @param file The path to the context file to use for the validation.
 * @param config The file's corresponding configuration entry.
 */
public record ContextFileData(Path file, ContextFileConfig config) {
}
