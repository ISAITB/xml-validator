/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.xml.validation.ValidationConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The validator application's configuration.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private final Map<String, String> defaultLabels = new HashMap<>();
    private String inputFilePrefix = "ITB-";
    private String reportFilePrefix = "TAR-";
    private Set<String> acceptedMimeTypes;
    private Set<String> acceptedSchematronExtensions;
    private Set<String> acceptedSchemaExtensions;
    private Set<String> acceptedZipMimeType;
    private Set<String> acceptedSchemaMimeType;
    private Set<String> acceptedSchematronMimeType;
    private boolean disablePreprocessingCache = false;
    private String defaultXmlDescription;
    private String defaultTypeDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultExternalSchemaDescription;
    private String defaultExternalSchematronDescription;
    private String defaultContextFilesDescription;
    private String defaultLocationAsPathDescription;
    private String defaultLocaleDescription;
    private String defaultAddInputToReportDescription;
    private String defaultShowLocationPathsDescription;

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
     * @return The web service description for the XML input.
     */
    public String getDefaultXmlDescription() {
        return defaultXmlDescription;
    }

    /**
     * @param defaultXmlDescription The web service description for the XML input.
     */
    public void setDefaultXmlDescription(String defaultXmlDescription) {
        this.defaultXmlDescription = defaultXmlDescription;
    }

    /**
     * @return The web service description for the type input.
     */
    public String getDefaultTypeDescription() {
        return defaultTypeDescription;
    }

    /**
     * @param defaultTypeDescription The web service description for the type input.
     */
    public void setDefaultTypeDescription(String defaultTypeDescription) {
        this.defaultTypeDescription = defaultTypeDescription;
    }

    /**
     * @return The web service description for the embedding method input.
     */
    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    /**
     * @param defaultEmbeddingMethodDescription The web service description for the embedding method input.
     */
    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    /**
     * @return The web service description for the external schema input.
     */
    public String getDefaultExternalSchemaDescription() {
        return defaultExternalSchemaDescription;
    }

    /**
     * @param defaultExternalSchemaDescription The web service description for the external Schema input.
     */
    public void setDefaultExternalSchemaDescription(String defaultExternalSchemaDescription) {
        this.defaultExternalSchemaDescription = defaultExternalSchemaDescription;
    }

    /**
     * @return The web service description for the external Schematron.
     */
    public String getDefaultExternalSchematronDescription() {
        return defaultExternalSchematronDescription;
    }

    /**
     * @param defaultExternalSchematronDescription The web service description for the external Schematron input.
     */
    public void setDefaultExternalSchematronDescription(String defaultExternalSchematronDescription) {
        this.defaultExternalSchematronDescription = defaultExternalSchematronDescription;
    }

    /**
     * @return The web service description for the context files input.
     */
    public String getDefaultContextFilesDescription() {
        return defaultContextFilesDescription;
    }

    /**
     * @param defaultContextFilesDescription The web service description for the context files input.
     */
    public void setDefaultContextFilesDescription(String defaultContextFilesDescription) {
        this.defaultContextFilesDescription = defaultContextFilesDescription;
    }

    /**
     * @return The web service description for the location as path input.
     */
    public String getDefaultLocationAsPathDescription() {
        return defaultLocationAsPathDescription;
    }

    /**
     * @param defaultLocationAsPathDescription The web service description for the location as path input.
     */
    public void setDefaultLocationAsPathDescription(String defaultLocationAsPathDescription) {
        this.defaultLocationAsPathDescription = defaultLocationAsPathDescription;
    }

    /**
     * @return The web service description for the locale input.
     */
    public String getDefaultLocaleDescription() {
        return defaultLocaleDescription;
    }

    /**
     * @param defaultLocaleDescription The web service description for the locale input.
     */
    public void setDefaultLocaleDescription(String defaultLocaleDescription) {
        this.defaultLocaleDescription = defaultLocaleDescription;
    }

    /**
     * @return The web service description for the add input to report input.
     */
    public String getDefaultAddInputToReportDescription() {
        return defaultAddInputToReportDescription;
    }

    /**
     * @param defaultAddInputToReportDescription The web service description for the add input to report input.
     */
    public void setDefaultAddInputToReportDescription(String defaultAddInputToReportDescription) {
        this.defaultAddInputToReportDescription = defaultAddInputToReportDescription;
    }

    /**
     * @return The web service description for the show location paths input.
     */
    public String getDefaultShowLocationPathsDescription() {
        return defaultShowLocationPathsDescription;
    }

    /**
     * @param defaultShowLocationPathsDescription The web service description for the show location paths input.
     */
    public void setDefaultShowLocationPathsDescription(String defaultShowLocationPathsDescription) {
        this.defaultShowLocationPathsDescription = defaultShowLocationPathsDescription;
    }

    /**
     * @return The default labels to use for the description of SOAP web service inputs.
     */
    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    /**
     * Initialise the configuration.
     */
    @Override
    @PostConstruct
    public void init() {
        super.init();
        setSupportsAdditionalInformationInReportItems(true);
        setSupportsTestDefinitionInReportItems(true);
        //  Default labels.
        defaultLabels.put(ValidationConstants.INPUT_XML, defaultXmlDescription);
        defaultLabels.put(ValidationConstants.INPUT_TYPE, defaultTypeDescription);
        defaultLabels.put(ValidationConstants.INPUT_EMBEDDING_METHOD, defaultEmbeddingMethodDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMA, defaultExternalSchemaDescription);
        defaultLabels.put(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, defaultExternalSchematronDescription);
        defaultLabels.put(ValidationConstants.INPUT_CONTEXT_FILES, defaultContextFilesDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCATION_AS_PATH, defaultLocationAsPathDescription);
        defaultLabels.put(ValidationConstants.INPUT_LOCALE, defaultLocaleDescription);
        defaultLabels.put(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, defaultAddInputToReportDescription);
        defaultLabels.put(ValidationConstants.INPUT_SHOW_LOCATION_PATHS, defaultShowLocationPathsDescription);
    }

}
