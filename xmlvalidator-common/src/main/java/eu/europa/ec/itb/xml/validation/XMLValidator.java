package eu.europa.ec.itb.xml.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportItemComparator;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Component used to validate XML against XML Schema and Schematron files.
 */
@Component
@Scope("prototype")
public class XMLValidator {

    private static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);

    @Autowired
    private FileManager fileManager;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private DomainPluginConfigProvider<DomainConfig> pluginConfigProvider;
    @Autowired
    private ApplicationContext ctx;

    private final File inputToValidate;
    private final DomainConfig domainConfig;
    private final LocalisationHelper localiser;
    private String validationType;
    private final ObjectFactory gitbTRObjectFactory = new ObjectFactory();
    private final List<FileInfo> externalSchema;
    private final List<FileInfo> externalSch;
    private final boolean locationAsPath;
    private final boolean addInputToReport;

    /**
     * Constructor.
     *
     * @param inputToValidate The input content to validate.
     * @param validationType The validation type.
     * @param externalSchema User-provided XSDs.
     * @param externalSch User-provided Schematron files.
     * @param domainConfig The domain configuration.
     * @param localiser Helper class for translations.
     */
    public XMLValidator(File inputToValidate, String validationType, List<FileInfo> externalSchema, List<FileInfo> externalSch, DomainConfig domainConfig, LocalisationHelper localiser) {
        this(inputToValidate, validationType, externalSchema, externalSch, domainConfig, false, true, localiser);
    }

    /**
     * Constructor.
     *
     * @param inputToValidate The input content to validate.
     * @param validationType The validation type.
     * @param externalSchema User-provided XSDs.
     * @param externalSch User-provided Schematron files.
     * @param domainConfig The domain configuration.
     * @param locationAsPath True if report item locations should be XPath expressions. If not the line numbers will be
     *                       calculated and recorded instead.
     * @param addInputToReport True if the provided input should be added as context to the produced TAR report.
     * @param localiser Helper class for translations.
     */
    public XMLValidator(File inputToValidate, String validationType, List<FileInfo> externalSchema, List<FileInfo> externalSch, DomainConfig domainConfig, boolean locationAsPath, boolean addInputToReport, LocalisationHelper localiser) {
        this.inputToValidate = inputToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalSchema = externalSchema;
        this.externalSch = externalSch;
        this.locationAsPath = locationAsPath;
        this.addInputToReport = addInputToReport;
        this.localiser = localiser;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    /**
     * Open a stream to read the input content.
     *
     * @return The stream to read.
     */
    private InputStream getInputStreamForValidation() {
        try {
            return Files.newInputStream(inputToValidate.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create an XSD resolver.
     *
     * @param xsdExternalPath The string absolute file system path to the location where externally loaded XSDs are placed.
     * @return The resolver.
     */
    private LSResourceResolver getXSDResolver(String xsdExternalPath) {
        return ctx.getBean(XSDFileResolver.class, validationType, domainConfig, xsdExternalPath);
    }

    /**
     * Create a URI resolver for Schematron files.
     *
     * @param schematronFile The Schematron file.
     * @return The resolver.
     */
    private javax.xml.transform.URIResolver getURIResolver(File schematronFile) {
        return ctx.getBean(URIResolver.class, validationType, schematronFile, domainConfig);
    }

    /**
     * @return The current domain identifier.
     */
    public String getDomain(){
        return this.domainConfig.getDomain();
    }

    /**
     * @return The current validation type.
     */
    public String getValidationType(){
        return this.validationType;
    }

    /**
     * Validate the input against the configured and provided XSDs.
     *
     * @return The TAR validation report.
     */
    private TAR validateAgainstSchema() {
        List<FileInfo> schemaFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA);
        schemaFiles.addAll(externalSchema);
        if (schemaFiles.isEmpty()) {
            logger.info("No schemas to validate against");
            return null;
        } else {
            List<TAR> reports = new ArrayList<>();
            for (FileInfo aSchemaFile: schemaFiles) {
                logger.info("Validating against [{}]", aSchemaFile.getFile().getName());
                TAR report = validateSchema(getInputStreamForValidation(), aSchemaFile.getFile());
                logReport(report, aSchemaFile.getFile().getName());
                reports.add(report);
                logger.info("Validated against [{}]", aSchemaFile.getFile().getName());
            }
            return Utils.mergeReports(reports);
        }
    }

    /**
     * Validate the input against a single XSD.
     *
     * @param inputSource The input to validate.
     * @param schemaFile The XSD to use.
     *
     * @return The TAR validation report.
     */
    private TAR validateSchema(InputStream inputSource, File schemaFile) {
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler();
        // Resolve schema.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(getXSDResolver(schemaFile.getParent()));
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
            validator.setProperty("http://apache.org/xml/properties/locale", localiser.getLocale());
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException("Unable to pass locale to validator", e);
        }
        validator.setErrorHandler(handler);
        TAR report;
        try {
            // Use a StreamSource rather than a DomSource below to get the line & column number of possible errors.
            StreamSource source = new StreamSource(inputSource);
            validator.validate(source);
            report = handler.createReport();
        } catch (Exception e) {
            logger.warn("Error while validating XML [{}]", e.getMessage());
            report = createFailureReport();
        }
        return report;
    }

    /**
     * Create an empty TAR report.
     *
     * @return The report.
     */
    private TAR createEmptyReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.SUCCESS);
        return report;
    }

    /**
     * Create a basic error TAR report for a core XML parsing problem.
     *
     * @return The TAR report.
     */
    private TAR createFailureReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.FAILURE);
        BAR error1 = new BAR();
        error1.setDescription(localiser.localise("validator.label.exception.errorDueToProblemInXML"));
        error1.setLocation("XML:1:0");
        var element1 = this.gitbTRObjectFactory.createTestAssertionGroupReportsTypeError(error1);
        report.getReports().getInfoOrWarningOrError().add(element1);
        return report;
    }

    /**
     * Complete the metadata of the provided report.
     *
     * @param report The report to complete.
     */
    private void completeReport(TAR report) {
        if (report != null) {
            if (report.getDate() == null) {
                report.setDate(Utils.getXMLGregorianCalendarDateTime());
            }
            if (addInputToReport && report.getContext() == null) {
                report.setContext(new AnyContent());
                String inputXML;
                try {
                    inputXML = StreamUtils.copyToString(getInputStreamForValidation(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                AnyContent input = new AnyContent();
                input.setValue(inputXML);
                input.setName(ValidationConstants.INPUT_XML);
                input.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
                input.setMimeType("application/xml");
                report.getContext().getItem().add(input);
            }
            if (report.getCounters() == null) {
                report.setCounters(new ValidationCounters());
                int infos = 0;
                int warnings = 0;
                int errors = 0;
                for (JAXBElement<TestAssertionReportType> item: report.getReports().getInfoOrWarningOrError()) {
                    String itemName = item.getName().getLocalPart();
                    if ("info".equals(itemName)) {
                        infos += 1;
                    } else if ("warning".equals(itemName)) {
                        warnings += 1;
                    } else if ("error".equals(itemName)) {
                        errors += 1;
                    }
                }
                report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
                report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
                report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
            }
        }
    }

    /**
     * Validate the input against the configured and provided Schematron files.
     *
     * @return The TAR validation report.
     */
    private TAR validateAgainstSchematron() {
        List<TAR> reports = new ArrayList<>();
        List<FileInfo> schematronFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON);
        schematronFiles.addAll(externalSch);
        if (schematronFiles.isEmpty()) {
            logger.info("No schematrons to validate against");
            return null;
        } else {
            for (FileInfo aSchematronFile: schematronFiles) {
                logger.info("Validating against [{}]", aSchematronFile.getFile().getName());
                TAR report = validateSchematron(getInputStreamForValidation(), aSchematronFile.getFile());
                logReport(report, aSchematronFile.getFile().getName());
                reports.add(report);
                logger.info("Validated against [{}]", aSchematronFile.getFile().getName());
            }
            return Utils.mergeReports(reports);
        }
    }

    /**
     * Log the validation output (if at debug level).
     *
     * @param report The report.
     * @param name The input file name.
     */
    private void logReport(TAR report, String name) {
        if (logger.isDebugEnabled()) {
            StringBuilder logOutput = new StringBuilder();
            logOutput.append("[").append(name).append("]\n Result: ").append(report.getResult());
            if (report.getCounters() != null) {
                logOutput.append("\nOverview: total: ").append(report.getCounters().getNrOfAssertions())
                        .append(" errors: ").append(report.getCounters().getNrOfErrors())
                        .append(" warnings: ").append(report.getCounters().getNrOfWarnings());
            }
            logOutput.append("\nDetails");
            report.getReports().getInfoOrWarningOrError().forEach(item -> {
                if (item.getValue() instanceof BAR) {
                    BAR reportItem = (BAR)item.getValue();
                    logOutput.append("\nDescription: ").append(reportItem.getDescription());
                }
            });
            logger.debug(logOutput.toString());
        }
    }

    /**
     * Validate the input XML against all configured and provided XSDs, Schematron files as well as custom
     * validator plugins.
     *
     * @return The TAR validation report.
     */
    public TAR validateAll() {
        TAR overallResult;
        try {
            fileManager.signalValidationStart(domainConfig.getDomainName());
            TAR schemaResult = validateAgainstSchema();
            if (schemaResult == null) {
                // No schema.
                schemaResult = createEmptyReport();
            }
            if (schemaResult.getResult() != TestResultType.SUCCESS) {
                overallResult = schemaResult;
            } else {
                TAR schematronResult = validateAgainstSchematron();
                if (schematronResult != null) {
                    overallResult = Utils.mergeReports(new TAR[] {schemaResult, schematronResult});
                } else {
                    overallResult = Utils.mergeReports(new TAR[] {schemaResult});
                }
            }
            completeReport(overallResult);
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
        TAR pluginResult = validateAgainstPlugins();
        if (pluginResult != null) {
            overallResult = Utils.mergeReports(new TAR[] {overallResult, pluginResult});
        }
        if (domainConfig.isReportsOrdered() && overallResult.getReports() != null) {
            overallResult.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
        }
        return overallResult;
    }

    /**
     * Validate the input against any configured custom plugins.
     *
     * @return The plugin validation report.
     */
    private TAR validateAgainstPlugins() {
        TAR pluginReport = null;
        ValidationPlugin[] plugins = pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(domainConfig, validationType));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(inputToValidate.getParentFile(), UUID.randomUUID().toString());
            try {
                pluginTmpFolder.mkdirs();
                ValidateRequest pluginInput = preparePluginInput(pluginTmpFolder);
                for (ValidationPlugin plugin: plugins) {
                    String pluginName = plugin.getName();
                    ValidationResponse response = plugin.validate(pluginInput);
                    if (response != null && response.getReport() != null && response.getReport().getReports() != null) {
                        logger.info("Plugin [{}] produced [{}] report item(s).", pluginName, response.getReport().getReports().getInfoOrWarningOrError().size());
                        if (pluginReport == null) {
                            pluginReport = response.getReport();
                        } else {
                            pluginReport = Utils.mergeReports(new TAR[] {pluginReport, response.getReport()});
                        }
                    }
                }
            } finally {
                // Cleanup plugin tmp folder.
                FileUtils.deleteQuietly(pluginTmpFolder);
            }
        }
        return pluginReport;
    }

    /**
     * Prepare the input to provide for plugin validation.
     *
     * @param pluginTmpFolder A temporary folder to be used by the plugins for processing.
     * @return The request to pass to each plugin.
     */
    private ValidateRequest preparePluginInput(File pluginTmpFolder) {
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID() +".xml");
        try {
            FileUtils.copyFile(inputToValidate, pluginInputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy input file for plugin", e);
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", domainConfig.getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", validationType));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("locale", localiser.getLocale().toString()));
        return request;
    }

    /**
     * Validate the XML input against a single Schematron file.
     *
     * @param inputSource The input to validate.
     * @param schematronFile The Schematron file.
     * @return The validation report.
     */
    private TAR validateSchematron(InputStream inputSource, File schematronFile) {
        Document schematronInput;
        SchematronOutputType svrlOutput;
        boolean convertXPathExpressions = false;
        String schematronFileName = schematronFile.getName().toLowerCase();
        ISchematronResource schematron;
        if (schematronFileName.endsWith("xslt") || schematronFileName.endsWith("xsl")) {
            // Validate as XSLT.
            schematron = SchematronResourceXSLT.fromFile(schematronFile);
            ((SchematronResourceXSLT) schematron).setURIResolver(getURIResolver(schematronFile));
        } else {
            // Validate as raw schematron.
            convertXPathExpressions = true;
            schematron = SchematronResourcePure.fromFile(schematronFile);
        }
        try {
            schematronInput = Utils.readXMLWithLineNumbers(inputSource);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse input file.", e);
        }
        try {
            Document svrlDocument = schematron.applySchematronValidation(new DOMSource(schematronInput));
            if (svrlDocument == null) {
                throw new IllegalStateException("SVRL output was null");
            }
            SVRLMarshaller marshaller = new SVRLMarshaller(false);
            svrlOutput = marshaller.read(svrlDocument);
        } catch (Exception e) {
            throw new IllegalStateException("Schematron file ["+schematronFile.getName()+"] is invalid", e);
        }
        SchematronReportHandler handler = new SchematronReportHandler(schematronInput, svrlOutput, convertXPathExpressions, domainConfig.isIncludeTestDefinition(), locationAsPath, localiser);
        return handler.createReport();
    }

}
