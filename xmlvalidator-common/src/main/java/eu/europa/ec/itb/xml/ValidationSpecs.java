package eu.europa.ec.itb.xml;

import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
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

/**
 * Class used to wrap the specifications with which to carry out a validation.
 */
public class ValidationSpecs {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationSpecs.class);

    private File input;
    private File inputToUse;
    private LocalisationHelper localisationHelper;
    private DomainConfig domainConfig;
    private String validationType;
    private List<FileInfo> externalSchema;
    private List<FileInfo> externalSch;
    private List<ContextFileData> contextFiles;
    private boolean locationAsPath = false;
    private boolean addInputToReport = true;
    private Path tempFolder;
    private List<FileInfo> schematronFilesToUse;
    private ApplicationContext applicationContext;

    /**
     * Private constructor to prevent direct initialisation.
     */
    private ValidationSpecs() {}

    /**
     * @return The pretty-printed JSON content to validate.
     */
    public File getInput() {
        return input;
    }

    /**
     * Open a stream to read the input content.
     *
     * @return The stream to read.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    public InputStream getInputStreamForValidation() throws XMLInvalidException {
        try {
            return Files.newInputStream(getInputFileToUse().toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
        if (inputToUse == null) {
            var expression = domainConfig.getInputPreprocessorPerType().get(validationType);
            if (expression == null) {
                // No preprocessing needed.
                inputToUse = input;
            } else {
                inputToUse = new File(input.getParentFile(), UUID.randomUUID() + ".xml");
                // A preprocessing XPath expression has been provided for the given validation type.
                XPathExpression xPath;
                try {
                    xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath().compile(expression);
                } catch (XPathExpressionException e) {
                    throw new ValidatorException("validator.label.exception.invalidInputPreprocessingExpression");
                }
                try (
                        var inputStream = Files.newInputStream(input.toPath());
                        var outputStream = Files.newOutputStream(inputToUse.toPath())
                ) {
                    var result = xPath.evaluate(new StreamSource(inputStream), XPathConstants.NODE);
                    if (result instanceof NodeInfo) {
                        int resultKind = ((NodeInfo) result).getNodeKind();
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
                inputToUse = replaceFile(input, inputToUse);
            }
            // We now see if we need also to pretty-print.
            if (addInputToReport || !locationAsPath) {
                File prettyPrintedFile = new File(inputToUse.getParentFile(), UUID.randomUUID() + ".xml");
                try {
                    var document = Utils.secureDocumentBuilder().parse(new InputSource(new FileReader(inputToUse)));
                    var factory = Utils.secureTransformerFactory();
                    var transformer = factory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(document), new StreamResult(prettyPrintedFile));
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to pretty-print input", e);
                }
                replaceFile(inputToUse, prettyPrintedFile);
            }
        }
        return inputToUse;
    }

    /**
     * Get the XSDs to use for the validation.
     *
     * @param fileManager The file manager to consider.
     * @return The list of XSDs.
     */
    public List<FileInfo> getSchemasToUse(FileManager fileManager) {
        List<FileInfo> schemaFiles = fileManager.getPreconfiguredValidationArtifacts(getDomainConfig(), getValidationType(), DomainConfig.ARTIFACT_TYPE_SCHEMA);
        schemaFiles.addAll(getExternalSchemas());
        return schemaFiles;
    }

    /**
     * Return the schematron files to use for the validation.
     * <p/>
     * The primary purpose of this method is to ensure that is we have user-provided context files as input to the current validation
     * run, these will be resolvable by preconfigured schematrons and that also each validation will use its only copy of
     * all artifacts to avoid conflicts.
     *
     * @param fileManager The file manager to use for artifact lookups.
     * @param appConfig The application configuration.
     * @return The list of schematron files to use.
     */
    public List<FileInfo> getSchematronsToUse(FileManager fileManager, ApplicationConfig appConfig) throws XMLInvalidException {
        if (schematronFilesToUse == null) {
            if (getContextFiles().isEmpty()) {
                // No context files. Return preconfigured and user-provided schematrons from their normal locations.
                List<FileInfo> schematronFiles = fileManager.getPreconfiguredValidationArtifacts(getDomainConfig(), getValidationType(), DomainConfig.ARTIFACT_TYPE_SCHEMATRON);
                schematronFiles.addAll(getExternalSchematrons());
                schematronFilesToUse = schematronFiles;
            } else {
                // We have context files. We need to copy everything in a temp folder specific to this validation run.
                validateContextFiles();
                List<FileInfo> schematronFiles = new ArrayList<>();
                Path originalDomain = Path.of(appConfig.getResourceRoot(), getDomainConfig().getDomain());
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
                        schematronFiles.add(new FileInfo(pathInTempDomainFolder.toFile(), originalFileInfo.getType()));
                    }
                    // Move user-provided schematron files to the temp domain folder.
                    for (var originalFileInfo: getExternalSchematrons()) {
                        Path originalPath = originalFileInfo.getFile().toPath();
                        Path pathInTempDomainFolder = Path.of(tmpDomain.toString(), originalPath.getFileName().toString());
                        Files.move(originalPath, pathInTempDomainFolder);
                        schematronFiles.add(new FileInfo(pathInTempDomainFolder.toFile(), originalFileInfo.getType()));
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
     * Get the path for a temporary copy of the domain configuration folder.
     *
     * @return The path.
     */
    private Path tempDomainPath() {
        return Path.of(getTempFolder().toString(), getDomainConfig().getDomain()).toAbsolutePath();
    }

    /**
     * Get the resource root to consider when resolving files within the domain.
     *
     * @param appConfig The application's configuration.
     * @return The absolute path as a string.
     */
    public String getResourceRootForDomainFileResolution(ApplicationConfig appConfig) {
        if (getContextFiles().isEmpty()) {
            return Path.of(appConfig.getResourceRoot(), getDomainConfig().getDomain()).toFile().getAbsolutePath();
        } else {
            return tempDomainPath().toFile().getAbsolutePath();
        }
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
                    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    schemaFactory.setResourceResolver(applicationContext.getBean(XSDFileResolver.class, getValidationType(), getDomainConfig(), schemaFile.getParent()));
                    Schema schema;
                    try {
                        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                        schema = schemaFactory.newSchema(new StreamSource(new FileInputStream(schemaFile)));
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    // Validate XML content against given XSD schema.
                    Validator validator = schema.newValidator();
                    try {
                        validator.setProperty("http://apache.org/xml/properties/locale", getLocalisationHelper().getLocale());
                    } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                        throw new IllegalStateException("Unable to pass locale to validator", e);
                    }
                    XSDReportHandler handler = new XSDReportHandler();
                    validator.setErrorHandler(handler);
                    boolean contextFileIsValid = false;
                    Optional<BAR> errorToReport = Optional.empty();
                    try (var inputSource = Files.newInputStream(file.file())) {
                        // Use a StreamSource rather than a DomSource below to get the line & column number of possible errors.
                        StreamSource source = new StreamSource(inputSource);
                        validator.validate(source);
                        TAR report = handler.createReport();
                        if (report.getResult() == TestResultType.SUCCESS) {
                            contextFileIsValid = true;
                        } else {
                            var firstError = report.getReports().getInfoOrWarningOrError().stream().filter(item -> "error".equals(item.getName().getLocalPart())).findFirst();
                            if (firstError.isPresent() && firstError.get().getValue() instanceof BAR errorItem) {
                                errorToReport = Optional.of(errorItem);
                            }
                        }
                    } catch (Exception e) {
                        LOG.info("Context file could not be parsed.", e);
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
         * Set the temporary folder used for this validation run.
         *
         * @param tempFolder The temporary folder.
         * @return The builder.
         */
        public Builder withTempFolder(Path tempFolder) {
            instance.tempFolder = tempFolder;
            return this;
        }

    }

}
