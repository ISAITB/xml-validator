package eu.europa.ec.itb.xml.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private ValidationConstants() { throw new IllegalStateException("Utility class"); }

    /** The validation type. */
    public static final String INPUT_TYPE = "type";
    /** The XML content to validate. */
    public static final String INPUT_XML = "xml";
    /** The explicit content embedding method. */
    public static final String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** User-provided XSD wrapper. */
    public static final String INPUT_EXTERNAL_SCHEMA = "externalSchema";
    /** User-provided Schematron wrapper. */
    public static final String INPUT_EXTERNAL_SCHEMATRON = "externalSchematron";
    /** The content of the user-provided artifact. */
    public static final String INPUT_EXTERNAL_ARTIFACT_CONTENT = "content";
    /** The type of provided artifact. */
    public static final String INPUT_EXTERNAL_ARTIFACT_TYPE = "type";
    /** Whether location information in errors should be a XPath expression. */
    public static final String INPUT_LOCATION_AS_PATH = "locationAsPath";
    /** Whether the validated content should be added to the TAR report. */
    public static final String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** The locale string to consider. */
    public static final String INPUT_LOCALE = "locale";

}
