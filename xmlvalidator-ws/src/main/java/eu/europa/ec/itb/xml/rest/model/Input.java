package eu.europa.ec.itb.xml.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.web.rest.model.SchemaInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The input to trigger a new validation via the validator's REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The content and metadata specific to input content that is to be validated.")
public class Input {

    @Schema(description = "The content to validate, provided as a normal string, a URL, or a BASE64-encoded string.")
    private String contentToValidate;
    @Schema(description = "The way in which to interpret the contentToValidate. If not provided, the method will be determined from the contentToValidate value.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64)
    private String embeddingMethod;
    @Schema(description = "The type of validation to perform (e.g. the profile to apply or the version to validate against). This can be skipped if a single validation type is supported by the validator. Otherwise, if multiple are supported, the service should fail with an error.")
    private String validationType;
    @Schema(description = "Any user-provided XSDs to consider for the validation (i.e. provided at the time of the call).")
    private List<SchemaInfo> externalSchemas;
    @Schema(description = "Any user-provided Schematrons to consider for the validation (i.e. provided at the time of the call).")
    private List<SchemaInfo> externalSchematrons;
    @Schema(description = "Whether the locations reported for returned errors will be XPath expressions (default true). False will return the line number in the input.", defaultValue = "true")
    private Boolean locationAsPath;
    @Schema(description = "Whether to include the validated input in the resulting report's context section.", defaultValue = "false")
    private Boolean addInputToReport;
    @Schema(description = "Locale (language code) to use for reporting of results. If the provided locale is not supported by the validator the default locale will be used instead (e.g. 'fr', 'fr_FR').")
    private String locale;

    /**
     * @return The string representing the content to validate (string as-is, URL or base64 content).
     */
    public String getContentToValidate() { return this.contentToValidate; }

    /**
     * @return The embedding method to consider to determine how the provided content input is to be processed.
     */
    public String getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @return The validation type to trigger for this domain.
     */
    public String getValidationType() { return this.validationType; }

    /**
     * @param contentToValidate The string representing the content to validate (string as-is, URL or base64 content).
     */
    public void setContentToValidate(String contentToValidate) {
        this.contentToValidate = contentToValidate;
    }

    /**
     * @param embeddingMethod  The embedding method to consider to determine how the provided content input is to be processed.
     */
    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * @param validationType The validation type to trigger for this domain.
     */
    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    /**
     * @return The set of user-provided schemas.
     */
    public List<SchemaInfo> getExternalSchemas() {
        return externalSchemas;
    }

    /**
     * @param externalSchemas The set of user-provided schemas.
     */
    public void setExternalSchemas(List<SchemaInfo> externalSchemas) {
        this.externalSchemas = externalSchemas;
    }

    /**
     * @return The set of user-provided schematrons.
     */
    public List<SchemaInfo> getExternalSchematrons() {
        return externalSchematrons;
    }

    /**
     * @param externalSchematrons The set of user-provided schematrons.
     */
    public void setExternalSchematrons(List<SchemaInfo> externalSchematrons) {
        this.externalSchematrons = externalSchematrons;
    }

    /**
     * @return Whether the location reported for returned errors will be a JSON pointer.
     */
    public Boolean getLocationAsPath() {
        return locationAsPath;
    }

    public void setLocationAsPath(Boolean locationAsPath) {
        this.locationAsPath = locationAsPath;
    }

    /**
     * @return Whether to include the validated input in the resulting report's context section.
     */
    public Boolean getAddInputToReport() {
        return addInputToReport;
    }

    /**
     * @param addInputToReport Whether to include the validated input in the resulting report's context section.
     */
    public void setAddInputToReport(Boolean addInputToReport) {
        this.addInputToReport = addInputToReport;
    }

    /**
     * @return The locale string.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale The locale string to set.
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }
}
