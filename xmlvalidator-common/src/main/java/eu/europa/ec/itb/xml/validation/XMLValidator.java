package eu.europa.ec.itb.xml.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.sch.SchematronResourceSCH;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import com.helger.xml.transform.DefaultTransformURIResolver;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ReportItemComparator;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import eu.europa.ec.itb.xml.*;
import eu.europa.ec.itb.xml.util.FileManager;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;

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
    private ApplicationConfig appConfig;
    @Autowired
    private ApplicationContext ctx;

    private final ObjectFactory gitbTRObjectFactory = new ObjectFactory();
    private final ValidationSpecs specs;

    /**
     * Constructor.
     *
     * @param specs The specifications with which to carry out the validation.
     */
    public XMLValidator(ValidationSpecs specs) {
        this.specs = specs;
    }

    /**
     * Create an XSD resolver.
     *
     * @param xsdExternalPath The string absolute file system path to the location where externally loaded XSDs are placed.
     * @return The resolver.
     */
    private LSResourceResolver getXSDResolver(String xsdExternalPath) {
        return ctx.getBean(XSDFileResolver.class, specs.getValidationType(), specs.getDomainConfig(), xsdExternalPath);
    }

    /**
     * Create a URI resolver for Schematron files.
     *
     * @param schematronFile The Schematron file.
     * @return The resolver.
     */
    private SchematronURIResolver getURIResolver(File schematronFile) {
        return new SchematronURIResolver(schematronFile);
    }

    /**
     * @return The current domain identifier.
     */
    public String getDomain(){
        return specs.getDomainConfig().getDomain();
    }

    /**
     * @return The current validation type.
     */
    public String getValidationType(){
        return specs.getValidationType();
    }

    /**
     * Validate the input against the configured and provided XSDs.
     *
     * @return The TAR validation report.
     */
    private TAR validateAgainstSchema() throws XMLInvalidException {
        List<FileInfo> schemaFiles = specs.getSchemasToUse(fileManager);
        if (schemaFiles.isEmpty()) {
            logger.info("No schemas to validate against");
            return null;
        } else {
            List<TAR> reports = new ArrayList<>();
            for (FileInfo aSchemaFile: schemaFiles) {
                logger.info("Validating against [{}]", aSchemaFile.getFile().getName());
                TAR report = validateSchema(specs.getInputStreamForValidation(false), aSchemaFile.getFile());
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
     * @param inputStream The input to validate.
     * @param schemaFile The XSD to use.
     *
     * @return The TAR validation report.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    private TAR validateSchema(InputStream inputStream, File schemaFile) throws XMLInvalidException {
        // Validate XML content against given XSD schema.
        var errorHandler = new XSDReportHandler();
        try (var schemaStream = Files.newInputStream(schemaFile.toPath())) {
            secureSchemaValidation(inputStream, schemaStream, errorHandler, getXSDResolver(schemaFile.getParent()), specs.getLocalisationHelper().getLocale());
        } catch (Exception e) {
            throw new XMLInvalidException(e);
        }
        return errorHandler.createReport();
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
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.ONE);
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.FAILURE);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        BAR error1 = new BAR();
        error1.setDescription(specs.getLocalisationHelper().localise("validator.label.exception.errorDueToProblemInXML"));
        error1.setLocation("XML:1:0");
        var element1 = this.gitbTRObjectFactory.createTestAssertionGroupReportsTypeError(error1);
        report.getReports().getInfoOrWarningOrError().add(element1);
        return report;
    }

    /**
     * Complete the metadata of the provided report.
     *
     * @param report The report to complete.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    private void completeReport(TAR report) throws XMLInvalidException {
        if (report != null) {
            if (report.getDate() == null) {
                report.setDate(Utils.getXMLGregorianCalendarDateTime());
            }
            if (specs.isAddInputToReport() && report.getContext() == null) {
                report.setContext(new AnyContent());
                String inputXML;
                try {
                    inputXML = Files.readString(specs.getInputFileToReport().toPath(), StandardCharsets.UTF_8);
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
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    private TAR validateAgainstSchematron() throws XMLInvalidException {
        List<TAR> reports = new ArrayList<>();
        List<SchematronFileInfo> schematronFiles = specs.getSchematronsToUse();
        if (schematronFiles.isEmpty()) {
            logger.info("No schematrons to validate against");
            return null;
        } else {
            for (SchematronFileInfo aSchematronFile: schematronFiles) {
                logger.info("Validating against [{}]", aSchematronFile.getFile().getName());
                TAR report = validateSchematron(aSchematronFile.getFile(), aSchematronFile.isSupportPureValidation());
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
                if (item.getValue() instanceof BAR reportItem) {
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
            try {
                fileManager.signalValidationStart(specs.getDomainConfig().getDomainName());
                TAR schemaResult = validateAgainstSchema();
                if (schemaResult == null) {
                    // No schema.
                    schemaResult = createEmptyReport();
                }
                if (schemaResult.getResult() != TestResultType.SUCCESS && specs.getDomainConfig().stopOnXsdErrors(getValidationType())) {
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
                fileManager.signalValidationEnd(specs.getDomainConfig().getDomainName());
            }
            TAR pluginResult = validateAgainstPlugins();
            if (pluginResult != null) {
                overallResult = Utils.mergeReports(new TAR[] {overallResult, pluginResult});
            }
            if (specs.getDomainConfig().isReportsOrdered() && overallResult.getReports() != null) {
                overallResult.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
            }
        } catch (XMLInvalidException e) {
            logger.warn("Error while validating XML [{}]", e.getMessage());
            overallResult = createFailureReport();
        }
        specs.getDomainConfig().applyMetadata(overallResult, getValidationType());
        Utils.sanitizeIfNeeded(overallResult, specs.getDomainConfig());
        return overallResult;
    }

    /**
     * Validate the input against any configured custom plugins.
     *
     * @return The plugin validation report.
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    private TAR validateAgainstPlugins() throws XMLInvalidException {
        TAR pluginReport = null;
        ValidationPlugin[] plugins = pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(specs.getDomainConfig(), specs.getValidationType()));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(specs.getInputFileToUse().getParentFile(), UUID.randomUUID().toString());
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
     * @throws XMLInvalidException If the XML cannot be parsed.
     */
    private ValidateRequest preparePluginInput(File pluginTmpFolder) throws XMLInvalidException {
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID() +".xml");
        try {
            FileUtils.copyFile(specs.getInputFileToUse(), pluginInputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy input file for plugin", e);
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", specs.getDomainConfig().getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", specs.getValidationType()));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("locale", specs.getLocalisationHelper().getLocale().toString()));
        return request;
    }

    /**
     * Treat the schematron file as XSLT.
     *
     * @param schematronFile The schematron file.
     * @return The schematron.
     */
    private ISchematronResource schematronAsXSLT(File schematronFile) {
        var schematron = SchematronResourceXSLT.fromFile(schematronFile);
        var newResolver = getURIResolver(schematronFile);
        var resolver = schematron.getURIResolver();
        if (resolver instanceof DefaultTransformURIResolver defaultResolver) {
            newResolver.setDefaultBase(defaultResolver.getDefaultBase());
        }
        schematron.setURIResolver(newResolver);
        return schematron;
    }

    /**
     * Treat the schematron file as raw/pure schematron.
     *
     * @param schematronFile The schematron file.
     * @return The schematron.
     */
    private ISchematronResource schematronAsRaw(File schematronFile, boolean isExternallyProvided) {
        if (isExternallyProvided) {
            /*
             * The "pure" validation approach is preferable to parsing the SCH as it is much more performant.
             * The problem is that external functions (e.g. loading external documents via document()) are
             * not support in pure mode so this can't be applied in all cases. However, when the schematron
             * is provided as an external input, it is anyway not possible to provide additional files and
             * use such functions. In such cases we should be able to use the pure approach without issues.
             */
            return SchematronResourcePure.fromFile(schematronFile);
        } else {
            var schematron = SchematronResourceSCH.fromFile(schematronFile);
            var newResolver = getURIResolver(schematronFile);
            var resolver = schematron.getURIResolver();
            if (resolver instanceof DefaultTransformURIResolver defaultResolver) {
                newResolver.setDefaultBase(defaultResolver.getDefaultBase());
            }
            schematron.setURIResolver(newResolver);
            return schematron;
        }
    }

    /**
     * Apply the schematron validation.
     *
     * @param schematron The schematron to use.
     * @return The validation output.
     * @throws Exception If a schematron validation error occurred.
     */
    private SchematronOutputType applySchematron(ISchematronResource schematron) throws Exception {
        Document svrlDocument = schematron.applySchematronValidation(new DOMSource(specs.inputAsDocumentForSchematronValidation()));
        if (svrlDocument == null) {
            throw new IllegalArgumentException("SVRL output was null");
        }
        var marshaller = new SVRLMarshaller(false);
        return marshaller.read(svrlDocument);
    }

    /**
     * Validate the XML input against a single Schematron file.
     *
     * @param schematronFile The Schematron file.
     * @param supportPureValidationApproach Whether the schematron file can be processed using the 'pure' approach.
     * @return The validation report.
     */
    private TAR validateSchematron(File schematronFile, boolean supportPureValidationApproach) {
        SchematronOutputType svrlOutput;
        boolean convertXPathExpressions = false;
        String schematronFileName = schematronFile.getName().toLowerCase();
        try {
            if (schematronFileName.endsWith("xslt") || schematronFileName.endsWith("xsl")) {
                // Validate as XSLT.
                svrlOutput = applySchematron(schematronAsXSLT(schematronFile));
            } else if (schematronFileName.endsWith("sch")) {
                // Validate as raw schematron.
                convertXPathExpressions = supportPureValidationApproach;
                svrlOutput = applySchematron(schematronAsRaw(schematronFile, supportPureValidationApproach));
            } else {
                // We're not certain - validate as raw and if that fails validate as XSLT.
                try {
                    convertXPathExpressions = supportPureValidationApproach;
                    svrlOutput = applySchematron(schematronAsRaw(schematronFile, supportPureValidationApproach));
                } catch (Exception e) {
                    // Try also as XSLT.
                    convertXPathExpressions = false;
                    svrlOutput = applySchematron(schematronAsXSLT(schematronFile));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Schematron file ["+schematronFile.getName()+"] is invalid", e);
        }
        SchematronReportHandler handler = new SchematronReportHandler(specs.inputAsDocumentForSchematronValidation(), svrlOutput, convertXPathExpressions, specs.getDomainConfig().isIncludeTestDefinition(), specs.getDomainConfig().isIncludeAssertionID(), specs.isLocationAsPath(), specs.isShowLocationPaths(), specs.getLocalisationHelper());
        return handler.createReport();
    }

}
