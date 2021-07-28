package eu.europa.ec.itb.xml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * The validator application's configuration.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private String inputFilePrefix = "ITB-";
    private String reportFilePrefix = "TAR-";
    private Set<String> acceptedMimeTypes;
    private Set<String> acceptedSchematronExtensions;
    private Set<String> acceptedSchemaExtensions;
    private Set<String> acceptedZipMimeType;
    private Set<String> acceptedSchemaMimeType;
    private Set<String> acceptedSchematronMimeType;
    private boolean disablePreprocessingCache = false;

    /**
     * @return True if caching is disabled for artifact pre-processing.
     */
    public boolean isDisablePreprocessingCache() {
        return disablePreprocessingCache;
    }

    /**
     * @param disablePreprocessingCache True if caching is disabled for artifact pre-processing.
     */
    public void setDisablePreprocessingCache(boolean disablePreprocessingCache) {
        this.disablePreprocessingCache = disablePreprocessingCache;
    }

    /**
     * @return The prefix for stored input files.
     */
    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    /**
     * @param inputFilePrefix The prefix for stored input files.
     */
    public void setInputFilePrefix(String inputFilePrefix) {
        this.inputFilePrefix = inputFilePrefix;
    }

    /**
     * @return The prefix for stored report files.
     */
    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    /**
     * @param reportFilePrefix The prefix for stored report files.
     */
    public void setReportFilePrefix(String reportFilePrefix) {
        this.reportFilePrefix = reportFilePrefix;
    }

    /**
     * @return The set of accepted mime types for provided inputs.
     */
    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    /**
     * @param acceptedMimeTypes The set of accepted mime types for provided inputs.
     */
    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    /**
     * @return The file extensions that are accepted when scanning for Schematron rule files.
     */
    public Set<String> getAcceptedSchematronExtensions() {
        return acceptedSchematronExtensions;
    }

    /**
     * @param acceptedSchematronExtensions The file extensions that are accepted when scanning for Schematron rule files.
     */
    public void setAcceptedSchematronExtensions(Set<String> acceptedSchematronExtensions) {
        this.acceptedSchematronExtensions = acceptedSchematronExtensions;
    }

    /**
     * @return The file extensions that are accepted when scanning for XSD files.
     */
    public Set<String> getAcceptedSchemaExtensions() {
        return acceptedSchemaExtensions;
    }

    /**
     * @param acceptedSchemaExtensions The file extensions that are accepted when scanning for XSD files.
     */
    public void setAcceptedSchemaExtensions(Set<String> acceptedSchemaExtensions) {
        this.acceptedSchemaExtensions = acceptedSchemaExtensions;
    }

    /**
     * @return The set of accepted mime types for user-provided Schematron files.
     */
    public Set<String> getAcceptedSchematronMimeType() {
        return acceptedSchematronMimeType;
    }

    /**
     * @param acceptedSchematronMimeType The set of accepted mime types for user-provided Schematron files.
     */
    public void setAcceptedSchematronMimeType(Set<String> acceptedSchematronMimeType) {
        this.acceptedSchematronMimeType = acceptedSchematronMimeType;
    }

    /**
     * @return The set of accepted mime types for user-provided ZIP archives.
     */
	public Set<String> getAcceptedZipMimeType() {
		return acceptedZipMimeType;
	}

    /**
     * @param acceptedZipMimeType The set of accepted mime types for user-provided ZIP archives.
     */
	public void setAcceptedZipMimeType(Set<String> acceptedZipMimeType) {
		this.acceptedZipMimeType = acceptedZipMimeType;
	}

    /**
     * @return The set of accepted mime types for user-provided XSD files.
     */
	public Set<String> getAcceptedSchemaMimeType() {
		return acceptedSchemaMimeType;
	}

    /**
     * @param acceptedSchemaMimeType The set of accepted mime types for user-provided XSD files.
     */
	public void setAcceptedSchemaMimeType(Set<String> acceptedSchemaMimeType) {
		this.acceptedSchemaMimeType = acceptedSchemaMimeType;
	}

    /**
     * Initialise the configuration.
     */
    @PostConstruct
    public void init() {
        super.init();
    }

}
