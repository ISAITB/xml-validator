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

import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.validation.commons.BomStrippingReader;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XSDFileResolver;
import eu.europa.ec.itb.xml.validation.XSDReportHandler;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;

/**
 * Class used to wrap the specifications with which to carry out a validation.
 */
public class ValidationSpecs {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationSpecs.class);

    private File input;
    private File inputToUse;
    private File schematronInputToUse;
    private LocalisationHelper localisationHelper;
    private DomainConfig domainConfig;
    private String validationType;
    private List<FileInfo> externalSchema;
    private List<FileInfo> externalSch;
    private List<ContextFileData> contextFiles;
    private boolean locationAsPath = false;
    private boolean addInputToReport = true;
    private Boolean showLocationPaths;
    private Path tempFolder;
    private List<SchematronFileInfo> schematronFilesToUse;
    private ApplicationContext applicationContext;
    private XMLInputFactory xmlInputFactory;
    private TransformerFactory transformerFactory;
    private Document schematronInputAsDocument;
    private boolean validateAgainstSchematrons = true;
    private boolean validateAgainstPlugins = true;
    private boolean logProgress = true;

    /**
     * Private constructor to prevent direct initialisation.
     */
    private ValidationSpecs() {}

    /**
     * @return Whether validation progress should be logged.
     */
    public boolean isLogProgress() {
        return logProgress;
    }

    /**
     * @return Whether the input should be validated against schematrons.
     */
    public boolean isValidateAgainstSchematrons() {
        return validateAgainstSchematrons;
    }

    /**
     * @return Whether the input should be validated against custom plugins.
     */
    public boolean isValidateAgainstPlugins() {
        return validateAgainstPlugins;
    }

    /**
     * @return The pretty-printed JSON content to validate.
     */
    public File getInput() {
        return input;
    }

    /**
     * Open a stream to read the input content.
     *
     * @param forSchematronValidation Whether this is called to validate Schematron rules.
     * @return The stream to read.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    public InputStream getInputStreamForValidation(boolean forSchematronValidation) throws XMLInvalidException {
        try {
            return Files.newInputStream(getInputFileToUse(forSchematronValidation).toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the input file to include as part of the validator's report.
     *
     * @return The path to the input file.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    public File getInputFileToReport() throws XMLInvalidException {
        boolean schematronValidationTookPlace = schematronInputToUse != null;
        return getInputFileToUse(schematronValidationTookPlace);
    }

    /**
     * Get the input file to use for validations covering schema and schematron validations.
     * <p/>
     * This method applies any pre-processing needed, followed by pretty-printing (if pretty-printing would
     * be necessary).
     * <p/>
     * Note that in the case of Schematrons it could be that we also need to combine context files with the main input.
     * If so, this would take place after any pre-processing of the main input file.
     *
     * @return The preprocessed input.
     */
    public File getInputFileToUse(boolean forSchematronValidation) throws XMLInvalidException {
        if (inputToUse == null || schematronInputToUse == null && forSchematronValidation) {
            if (inputToUse == null) {
                // Apply first preprocessing using XPath and then XSLT transformation (if any of these is defined).
                FileProcessingResult preprocessingResult = applyInputPreprocessing(input);
                FileProcessingResult transformationResult = applyInputTransformation(preprocessingResult.fileToUse());
                inputToUse = transformationResult.fileToUse();
                // We now see if we need also to pretty-print.
                // Note that if we made a XSLT transformation the input is already pretty-printed.
                if ((addInputToReport || !locationAsPath) && !transformationResult.processingApplied()) {
                    prettyPrintFile(inputToUse);
                }
            }
            // The main input file is fully pre-processed at this point.
            if (forSchematronValidation && schematronInputToUse == null) {
                /*
                 * We may have a different Schematron input if we have both of these applying:
                 * 1. One or more context files configured for the selected validation type.
                 * 2. A context file combination template.
                 */
                // Make sure the context files are validated and placed in the expected locations.
                getSchematronsToUse();
                String validationType = getValidationType();
                List<ContextFileConfig> contextFilesToCombine =  domainConfig.getContextFiles(validationType)
                        .stream()
                        .filter(contextFile -> contextFile.combinationPlaceholder().isPresent())
                        .toList();
                Optional<ContextFileCombinationTemplateConfig> combinationTemplate = domainConfig.getContextFileCombinationTemplate(validationType);
                if (!contextFilesToCombine.isEmpty() && combinationTemplate.isPresent()) {
                    Path combinationTemplatePath = tempDomainPath().resolve(combinationTemplate.get().configuredPath());
                    // We need to combine the input and context file(s) based on the configured template.
                    String combinationXslt = getContextFileCombinationXslt(contextFilesToCombine);
                    File combinedInputFile = new File(inputToUse.getParentFile(), UUID.randomUUID() + ".xml");
                    applyXsltTransformation(combinationTemplatePath, () -> new StringReader(combinationXslt), combinedInputFile.toPath(), true);
                    schematronInputToUse = combinedInputFile;
                } else {
                    // Schematron input is unchanged.
                    schematronInputToUse = inputToUse;
                }
            }
        }
        return forSchematronValidation?schematronInputToUse:inputToUse;
    }

    /**
     * Apply an XSLT transformation.
     *
     * @param inputFile The file to transform.
     * @param xsltReader A function to supply the XSLT to make the transformation with (or null).
     * @param outputFile The resulting file.
     * @param exposeTransformationInError Whether the user should be made aware that a XSLT transformation was taking place.
     */
    private void applyXsltTransformation(Path inputFile, Supplier<Reader> xsltReader, Path outputFile, boolean exposeTransformationInError) {
        try (BomStrippingReader xmlReader = new BomStrippingReader(Files.newInputStream(inputFile))) {
            var reader = getXmlInputFactory().createXMLStreamReader(xmlReader);
            Transformer transformer;
            if (xsltReader == null) {
                transformer = getTransformerFactory().newTransformer();
            } else {
                transformer = getTransformerFactory().newTransformer(new StAXSource(getXmlInputFactory().createXMLStreamReader(xsltReader.get())));
            }
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new StAXSource(reader), new StreamResult(outputFile.toFile()));
        } catch (Exception e) {
            var errorMessage = new StringBuilder();
            if (exposeTransformationInError) {
                errorMessage.append("Unable to apply XSLT transformation");
            } else {
                errorMessage.append("Unable to process as XML");
            }
            var cause = e.getMessage();
            if (StringUtils.isNotBlank(cause)) {
                errorMessage.append(" [").append(cause.trim()).append(']');
            }
            throw new IllegalStateException(errorMessage.toString(), e);
        }
    }

    /**
     * Apply XPath input preprocessing to the input file (if needed).
     *
     * @param originalFile The original file.
     * @return The result.
     * @throws XMLInvalidException If the XML could not be parsed.
     */
    private FileProcessingResult applyInputPreprocessing(File originalFile) throws XMLInvalidException {
        File inputToReturn = originalFile;
        boolean processingApplied = false;
        var expression = domainConfig.getInputPreprocessorPerType().get(validationType);
        if (expression != null) {
            processingApplied = true;
            inputToReturn = new File(originalFile.getParentFile(), UUID.randomUUID() + ".xml");
            // A preprocessing XPath expression has been provided for the given validation type.
            XPathExpression xPath;
            try {
                xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath().compile(expression);
            } catch (XPathExpressionException e) {
                throw new ValidatorException("validator.label.exception.invalidInputPreprocessingExpression");
            }
            try (
                    var inputStream = Files.newInputStream(originalFile.toPath());
                    var outputStream = Files.newOutputStream(inputToReturn.toPath())
            ) {
                var result = xPath.evaluate(new StreamSource(inputStream), XPathConstants.NODE);
                if (result instanceof NodeInfo nodeInfo) {
                    int resultKind = nodeInfo.getNodeKind();
                    if (resultKind == Type.ELEMENT || resultKind == Type.DOCUMENT || resultKind == Type.NODE) {
                        Utils.serialize((Source) result, outputStream);
                    } else {
                        throw new ValidatorException("validator.label.exception.invalidInputPreprocessingResult");
                    }
                } else {
                    throw new ValidatorException("validator.label.exception.invalidInputPreprocessingResult");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read input for preprocessing", e);
            } catch (XPathExpressionException e) {
                throw new XMLInvalidException(e);
            }
            inputToReturn = replaceFile(originalFile, inputToReturn);
        }
        return new FileProcessingResult(inputToReturn, processingApplied);
    }

    /**
     * Apply XSLT input transformation to the input file (if needed).
     *
     * @param originalFile The original file.
     * @return The result.
     */
    private FileProcessingResult applyInputTransformation(File originalFile) {
        File inputToReturn = originalFile;
        boolean processingApplied = false;
        Path xsltPath = domainConfig.getInputTransformerMap().get(validationType);
        if (xsltPath != null) {
            // We have a transformation to apply.
            processingApplied = true;
            inputToReturn = new File(originalFile.getParentFile(), UUID.randomUUID() + ".xml");
            applyXsltTransformation(originalFile.toPath(), () -> {
                try {
                    return new BomStrippingReader(Files.newInputStream(xsltPath));
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read XSLT file for input transformation", e);
                }
            }, inputToReturn.toPath(), true);
            inputToReturn = replaceFile(originalFile, inputToReturn);
        }
        return new FileProcessingResult(inputToReturn, processingApplied);
    }

    /**
     * Construct the XSLT file to use for processing the context file combination template.
     *
     * @param contextFilesToCombine The context files to combine.
     * @return The XSLT content to use.
     */
    private String getContextFileCombinationXslt(List<ContextFileConfig> contextFilesToCombine) {
        var combinationXslt = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="xml" indent="yes"/>
                    <xsl:template match="@* | node()">
                      <xsl:copy>
                        <xsl:apply-templates select="@* | node()"/>
                      </xsl:copy>
                    </xsl:template>
                    <xsl:template match="*[normalize-space(string-join(./text())) = '${input}']/text()">
                        <xsl:copy-of select="document('%s')" />
                    </xsl:template>
                """.formatted(inputToUse.toURI().toString())
        );
        for (var contextFile: contextFilesToCombine) {
            if (contextFile.combinationPlaceholder().isPresent()) {
                Path contextFilePathForValidation = tempDomainPath().resolve(contextFile.configuredPath());
                combinationXslt.append("""
                    <xsl:template match="*[normalize-space(string-join(./text())) = '${%s}']/text()">
                        <xsl:copy-of select="document('%s')" />
                    </xsl:template>
                    """.formatted(contextFile.combinationPlaceholder().get(), contextFilePathForValidation.toUri().toString())
                );
            }
        }
        combinationXslt.append("</xsl:stylesheet>");
        return combinationXslt.toString();
    }

    /**
     * @return The overall application configuration.
     */
    private ApplicationConfig getApplicationConfig() {
        return applicationContext.getBean(ApplicationConfig.class);
    }

    /**
     * Pretty-print the provided file.
     *
     * @param fileToProcess The file to process.
     */
    private void prettyPrintFile(File fileToProcess) {
        File prettyPrintedFile = new File(fileToProcess.getParentFile(), UUID.randomUUID() + ".xml");
        applyXsltTransformation(fileToProcess.toPath(), null, prettyPrintedFile.toPath(), false);
        replaceFile(fileToProcess, prettyPrintedFile);
    }

    /**
     * Get the input file to use for validations.
     * </p>
     * This method applies any pre-processing needed, followed by pretty-printing (if pretty-printing would
     * be necessary).
     *
     * @return The preprocessed input.
     */
    public File getInputFileToUse() throws XMLInvalidException {
        return getInputFileToUse(false);
    }

    /**
     * Get the XSDs to use for the validation.
     *
     * @param fileManager The file manager to consider.
     * @return The list of XSDs.
     */
    public List<FileInfo> getSchemasToUse(FileManager fileManager) {
        List<FileInfo> schemaFiles = new ArrayList<>();
        List<FileInfo> preconfiguredSchemas = fileManager.getPreconfiguredValidationArtifacts(getDomainConfig(), getValidationType(), DomainConfig.ARTIFACT_TYPE_SCHEMA);
        if (preconfiguredSchemas != null) {
            schemaFiles.addAll(preconfiguredSchemas);
        }
        List<FileInfo> externalSchemas = getExternalSchemas();
        if (externalSchemas != null) {
            schemaFiles.addAll(externalSchemas);
        }
        return schemaFiles;
    }

    /**
     * Return the schematron files to use for the validation.
     * <p/>
     * The primary purpose of this method is to ensure that is we have user-provided context files as input to the current validation
     * run, these will be resolvable by preconfigured schematrons and that also each validation will use its only copy of
     * all artifacts to avoid conflicts.
     *
     * @return The list of schematron files to use.
     */
    public List<SchematronFileInfo> getSchematronsToUse() {
        if (schematronFilesToUse == null) {
            FileManager fileManager = applicationContext.getBean(FileManager.class);
            if (getContextFiles().isEmpty()) {
                // No context files. Return preconfigured and user-provided schematrons from their normal locations.
                List<SchematronFileInfo> schematronFiles = new ArrayList<>(fileManager.getPreconfiguredValidationArtifacts(getDomainConfig(), getValidationType(), DomainConfig.ARTIFACT_TYPE_SCHEMATRON)
                        .stream().map(fileInfo -> new SchematronFileInfo(fileInfo, false)).toList());
                schematronFiles.addAll(getExternalSchematrons().stream().map(fileInfo -> new SchematronFileInfo(fileInfo, true)).toList());
                schematronFilesToUse = schematronFiles;
            } else {
                // We have context files. We need to copy everything in a temp folder specific to this validation run.
                validateContextFiles();
                List<SchematronFileInfo> schematronFiles = new ArrayList<>();
                Path originalDomain = Path.of(getApplicationConfig().getResourceRoot(), getDomainConfig().getDomain());
                Path tmpDomain = tempDomainPath();
                try {
                    // Copy domain folder to temporary folder. We copy the entire folder as we can't know if specific schematrons refer to other resources from the domain as relative paths (e.g. imports).
                    FileUtils.copyDirectoryToDirectory(originalDomain.toFile(), getTempFolder().toFile());
                    // List the schematrons and copy them also if not already copied (we could have new files due to preprocessing).
                    List<FileInfo> originalFilesToUse = fileManager.getPreconfiguredValidationArtifacts(getDomainConfig(), getValidationType(), DomainConfig.ARTIFACT_TYPE_SCHEMATRON);
                    for (var originalFileInfo: originalFilesToUse) {
                        Path originalPath = originalFileInfo.getFile().toPath().toAbsolutePath();
                        Path pathInTempDomainFolder = Path.of(originalPath.toString().replace(originalDomain.toString(), tmpDomain.toString()));
                        if (!Files.exists(pathInTempDomainFolder)) {
                            Files.createDirectories(pathInTempDomainFolder.getParent());
                            Files.copy(originalPath, pathInTempDomainFolder);
                        }
                        schematronFiles.add(new SchematronFileInfo(pathInTempDomainFolder.toFile(), originalFileInfo.getType(), false));
                    }
                    // Move user-provided schematron files to the temp domain folder.
                    for (var originalFileInfo: getExternalSchematrons()) {
                        Path originalPath = originalFileInfo.getFile().toPath();
                        Path pathInTempDomainFolder = Path.of(tmpDomain.toString(), originalPath.getFileName().toString());
                        Files.move(originalPath, pathInTempDomainFolder);
                        schematronFiles.add(new SchematronFileInfo(pathInTempDomainFolder.toFile(), originalFileInfo.getType(), false));
                    }
                    // Move user-provided context files to domain folder.
                    for (var contextFile: getContextFiles()) {
                        Path originalPath = contextFile.file().toAbsolutePath();
                        Path pathToReplace = Path.of(getTempFolder().toString(), "contextFiles").toAbsolutePath();
                        Path pathInTempDomainFolder = Path.of(originalPath.toString().replace(pathToReplace.toString(), tmpDomain.toString()));
                        if (!Files.exists(pathInTempDomainFolder)) {
                            Files.createDirectories(pathInTempDomainFolder.getParent());
                            Files.move(originalPath, pathInTempDomainFolder);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to prepare artifacts for validation.", e);
                }
                schematronFilesToUse = schematronFiles;
            }
        }
        return schematronFilesToUse;
    }

    /**
     * @return The XML input factory to use.
     */
    private XMLInputFactory getXmlInputFactory() {
        if (xmlInputFactory == null) {
            xmlInputFactory = Utils.secureXMLInputFactory();
        }
        return xmlInputFactory;
    }

    /**
     * @return The transformer factory to use.
     */
    private TransformerFactory getTransformerFactory() {
        if (transformerFactory == null) {
            transformerFactory = TransformerFactory.newInstance();
        }
        return transformerFactory;
    }

    /**
     * Get the path for a temporary copy of the domain configuration folder.
     *
     * @return The path.
     */
    private Path tempDomainPath() {
        return Path.of(getTempFolder().toString(), getDomainConfig().getDomain()).toAbsolutePath();
    }

    /**
     * Replace one file with another.
     *
     * @param fileToReplace The file to replace.
     * @param newFile The new file.
     * @return The file path to use.
     */
    private File replaceFile(File fileToReplace, File newFile) {
        try {
            FileUtils.deleteQuietly(fileToReplace);
            FileUtils.moveFile(newFile, fileToReplace);
            return fileToReplace;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to replace file", e);
        }
    }

    /**
     * @return Helper class to facilitate translation lookups.
     */
    public LocalisationHelper getLocalisationHelper() {
        return localisationHelper;
    }

    /**
     * @return The current domain configuration.
     */
    public DomainConfig getDomainConfig() {
        return domainConfig;
    }

    /**
     * @return The requested validation type.
     */
    public String getValidationType() {
        if (validationType == null) {
            validationType = domainConfig.getType().get(0);
        }
        return validationType;
    }

    /**
     * @return The user-provided Schema files to use.
     */
    public List<FileInfo> getExternalSchemas() {
        return externalSchema;
    }

    /**
     * @return The user-provided Schematron files to use.
     */
    public List<FileInfo> getExternalSchematrons() {
        return externalSch;
    }

    /**
     * @return The user-provided context files to use.
     */
    public List<ContextFileData> getContextFiles() {
        return contextFiles;
    }

    /**
     * Validate the configured context files (if any).
     *
     * @throws ValidatorException If a context file fails XSD validation.
     */
    private void validateContextFiles() throws ValidatorException {
        var files = getContextFiles();
        if (files != null && !files.isEmpty()) {
            for (var file: files) {
                if (file.config().schema().isPresent()) {
                    var schemaFile = file.config().schema().get().toFile();
                    LOG.info("Validating context file against [{}]", schemaFile.getName());
                    // Validate XML content against given XSD schema.
                    var errorHandler = new XSDReportHandler();
                    try (
                            var inputStream = Files.newInputStream(file.file());
                            var schemaStream = Files.newInputStream(schemaFile.toPath())
                    ) {
                        secureSchemaValidation(
                                inputStream,
                                schemaStream,
                                errorHandler,
                                applicationContext.getBean(XSDFileResolver.class, getDomainConfig(), schemaFile.toURI()),
                                getLocalisationHelper().getLocale()
                        );
                    } catch (Exception e) {
                        throw new IllegalStateException("Context file could not be parsed.", e);
                    }
                    boolean contextFileIsValid = false;
                    Optional<BAR> errorToReport = Optional.empty();
                    TAR report = errorHandler.createReport();
                    if (report.getResult() == TestResultType.SUCCESS) {
                        contextFileIsValid = true;
                    } else {
                        var firstError = report.getReports().getInfoOrWarningOrError().stream().filter(item -> "error".equals(item.getName().getLocalPart())).findFirst();
                        if (firstError.isPresent() && firstError.get().getValue() instanceof BAR errorItem) {
                            errorToReport = Optional.of(errorItem);
                        }
                    }
                    if (!contextFileIsValid) {
                        StringBuilder msgBuilder = new StringBuilder();
                        // Add as a prefix the label of the specific context file (if defined).
                        msgBuilder.append('[');
                        if (file.config().hasLabel()) {
                            msgBuilder.append(getLocalisationHelper().localise("validator.contextFile.%s.%s.label".formatted(getValidationType(), file.config().index())));
                        } else {
                            msgBuilder.append(getLocalisationHelper().localise("validator.label.contextFileLabel"));
                        }
                        msgBuilder.append("] ");
                        // Add information for the first encountered error (which is enough as this is an XSD validation).
                        if (errorToReport.isPresent()) {
                            msgBuilder.append(errorToReport.get().getDescription());
                        } else {
                            msgBuilder.append(getLocalisationHelper().localise("validator.label.exception.contextFileFailedValidation"));
                        }
                        throw new ValidatorException(msgBuilder.toString(), true);
                    }
                }
            }
        }
    }

    /**
     * Get the input document as a DOM document with defined lined numbers.
     *
     * @return The document.
     */
    public Document inputAsDocumentForSchematronValidation() {
        if (schematronInputAsDocument == null) {
            try {
                schematronInputAsDocument = Utils.readXMLWithLineNumbers(getInputStreamForValidation(true));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to parse input file.", e);
            }
        }
        return schematronInputAsDocument;
    }

    /**
     * @return The temporary folder for this validation run.
     */
    public Path getTempFolder() {
        return tempFolder;
    }

    /**
     * @return True if the location for error messages should be XPath expressions.
     */
    public boolean isLocationAsPath() {
        return locationAsPath;
    }

    /**
     * @return True if a simplified XPath expression should be added to report item locations.
     */
    public boolean isShowLocationPaths() {
        if (showLocationPaths == null) {
            showLocationPaths = domainConfig.isIncludeLocationPath();
        }
        return showLocationPaths;
    }

    /**
     * @return True if the provided input should be added as context to the produced TAR report.
     */
    public boolean isAddInputToReport() {
        return addInputToReport;
    }

    /**
     * Build the validation specifications.
     *
     * @param input The JSON content to validate.
     * @param localisationHelper Helper class to facilitate translation lookups.
     * @param domainConfig The current domain configuration.
     * @param applicationContext The Spring application context.
     * @return The specification builder.
     */
    public static Builder builder(File input, LocalisationHelper localisationHelper, DomainConfig domainConfig, ApplicationContext applicationContext) {
        return new Builder(input, localisationHelper, domainConfig, applicationContext);
    }

    /**
     * Builder class used to incrementally create a specification instance.
     */
    public static class Builder {

        private final ValidationSpecs instance;

        /**
         * Constructor.
         *
         * @param input The JSON content to validate.
         * @param localisationHelper Helper class to facilitate translation lookups.
         * @param domainConfig The current domain configuration.
         * @param applicationContext The Spring application context.
         */
        Builder(File input, LocalisationHelper localisationHelper, DomainConfig domainConfig, ApplicationContext applicationContext) {
            instance = new ValidationSpecs();
            instance.input = input;
            instance.localisationHelper = localisationHelper;
            instance.domainConfig = domainConfig;
            instance.applicationContext = applicationContext;
        }

        /**
         * @return The specification instance to use.
         */
        public ValidationSpecs build() {
            return instance;
        }

        /**
         * @param validationType Set the validation type to consider.
         * @return The builder.
         */
        public Builder withValidationType(String validationType) {
            instance.validationType = validationType;
            return this;
        }

        /**
         * @param externalSchemas Set the user-provided schemas to consider.
         * @return The builder.
         */
        public Builder withExternalSchemas(List<FileInfo> externalSchemas) {
            instance.externalSchema = externalSchemas;
            return this;
        }

        /**
         * @param externalSchematrons Set the user-provided schemas to consider.
         * @return The builder.
         */
        public Builder withExternalSchematrons(List<FileInfo> externalSchematrons) {
            instance.externalSch = externalSchematrons;
            return this;
        }

        /**
         * @param contextFiles Set the user-provided context files to consider.
         * @return The builder.
         */
        public Builder withContextFiles(List<ContextFileData> contextFiles) {
            instance.contextFiles = contextFiles;
            return this;
        }

        /**
         * Set the report items' location as XPath expressions.
         * <p/>
         * If not the line numbers will be calculated and recorded instead.
         *
         * @param locationAsPath The flag's value.
         * @return The builder.
         */
        public Builder locationAsPath(boolean locationAsPath) {
            instance.locationAsPath = locationAsPath;
            return this;
        }

        /**
         * Add the validated input content to the detailed TAR report's context.
         *
         * @param addInputToReport The flag's value.
         * @return The builder.
         */
        public Builder addInputToReport(boolean addInputToReport) {
            instance.addInputToReport = addInputToReport;
            return this;
        }

        /**
         * Whether locations will include a simplified XPath expression.
         *
         * @param showLocationPaths The flag's value.
         * @return The builder.
         */
        public Builder showLocationPaths(boolean showLocationPaths) {
            instance.showLocationPaths = showLocationPaths;
            return this;
        }

        /**
         * Set the temporary folder used for this validation run.
         *
         * @param tempFolder The temporary folder.
         * @return The builder.
         */
        public Builder withTempFolder(Path tempFolder) {
            instance.tempFolder = tempFolder;
            return this;
        }

        /**
         * Skip schematron validation.
         *
         * @return The builder.
         */
        public Builder skipSchematronValidation() {
            instance.validateAgainstSchematrons = false;
            return this;
        }

        /**
         * Skip plugin validation.
         *
         * @return The builder.
         */
        public Builder skipPluginValidation() {
            instance.validateAgainstPlugins = false;
            return this;
        }

        /**
         * Skip progress logging.
         *
         * @return The builder instance.
         */
        public Builder skipProgressLogging() {
            this.instance.logProgress = false;
            return this;
        }

    }

    /**
     * Record to report the result of an input processing action.
     *
     * @param fileToUse The file to use as a result of the processing (may also be unchanged).
     * @param processingApplied Whether processing took place.
     */
    private record FileProcessingResult(File fileToUse, boolean processingApplied) {}
}
