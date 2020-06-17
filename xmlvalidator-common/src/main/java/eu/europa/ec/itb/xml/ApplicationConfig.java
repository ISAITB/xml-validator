package eu.europa.ec.itb.xml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private boolean standalone = false;
    private String inputFilePrefix = "ITB-";
    private String reportFilePrefix = "TAR-";
    private Set<String> acceptedMimeTypes;
    private Set<String> acceptedSchematronExtensions;
    private Set<String> acceptedSchemaExtensions;
    private Set<String> acceptedZipMimeType;
    private Set<String> acceptedSchemaMimeType;
    private Set<String> acceptedSchematronMimeType;
    private boolean disablePreprocessingCache = false;

    public boolean isDisablePreprocessingCache() {
        return disablePreprocessingCache;
    }

    public void setDisablePreprocessingCache(boolean disablePreprocessingCache) {
        this.disablePreprocessingCache = disablePreprocessingCache;
    }

    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    public void setInputFilePrefix(String inputFilePrefix) {
        this.inputFilePrefix = inputFilePrefix;
    }

    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    public void setReportFilePrefix(String reportFilePrefix) {
        this.reportFilePrefix = reportFilePrefix;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public Set<String> getAcceptedSchematronExtensions() {
        return acceptedSchematronExtensions;
    }

    public void setAcceptedSchematronExtensions(Set<String> acceptedSchematronExtensions) {
        this.acceptedSchematronExtensions = acceptedSchematronExtensions;
    }

    public Set<String> getAcceptedSchemaExtensions() {
        return acceptedSchemaExtensions;
    }

    public void setAcceptedSchemaExtensions(Set<String> acceptedSchemaExtensions) {
        this.acceptedSchemaExtensions = acceptedSchemaExtensions;
    }

    public Set<String> getAcceptedSchematronMimeType() {
        return acceptedSchematronMimeType;
    }

    public void setAcceptedSchematronMimeType(Set<String> acceptedSchematronMimeType) {
        this.acceptedSchematronMimeType = acceptedSchematronMimeType;
    }

	public Set<String> getAcceptedZipMimeType() {
		return acceptedZipMimeType;
	}

	public void setAcceptedZipMimeType(Set<String> acceptedZipMimeType) {
		this.acceptedZipMimeType = acceptedZipMimeType;
	}

	public Set<String> getAcceptedSchemaMimeType() {
		return acceptedSchemaMimeType;
	}

	public void setAcceptedSchemaMimeType(Set<String> acceptedSchemaMimeType) {
		this.acceptedSchemaMimeType = acceptedSchemaMimeType;
	}

    @PostConstruct
    public void init() {
        super.init();
    }

}
