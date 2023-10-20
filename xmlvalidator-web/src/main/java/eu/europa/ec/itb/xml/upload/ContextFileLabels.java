package eu.europa.ec.itb.xml.upload;

/**
 * The configured labels linked to a context file.
 *
 * @param label The label for the context file controls.
 * @param placeholder The label for the upload placeholder text.
 */
public record ContextFileLabels(String label, String placeholder) {
}
