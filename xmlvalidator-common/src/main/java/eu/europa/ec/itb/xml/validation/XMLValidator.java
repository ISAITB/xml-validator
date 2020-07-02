package eu.europa.ec.itb.xml.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by simatosc on 26/02/2016.
 */
@Component
@Scope("prototype")
public class XMLValidator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);
    private static JAXBContext SVRL_JAXB_CONTEXT;

    @Autowired
    private FileManager fileManager;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private DomainPluginConfigProvider pluginConfigProvider;

    private File inputToValidate;
    private ApplicationContext ctx;
    private final DomainConfig domainConfig;
    private String validationType;
    private ObjectFactory gitbTRObjectFactory = new ObjectFactory();
    private List<FileInfo> externalSchema;
    private List<FileInfo> externalSch;

    static {
        try {
            SVRL_JAXB_CONTEXT = JAXBContext.newInstance(SchematronOutputType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB content for SchematronOutputType", e);
        }
    }

    public XMLValidator(File inputToValidate, String validationType, List<FileInfo> externalSchema, List<FileInfo> externalSch, DomainConfig domainConfig) {
        this.inputToValidate = inputToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalSchema = externalSchema;
        this.externalSch = externalSch;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    private InputStream getInputStreamForValidation() {
        try {
            return Files.newInputStream(inputToValidate.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private LSResourceResolver getXSDResolver(String xsdExternalPath) {
        return ctx.getBean(XSDFileResolver.class, validationType, domainConfig, xsdExternalPath);
    }

    private javax.xml.transform.URIResolver getURIResolver(File schematronFile) {
        return ctx.getBean(URIResolver.class, validationType, schematronFile, domainConfig);
    }

    private TAR validateAgainstSchema() {
        List<FileInfo> schemaFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA);
        schemaFiles.addAll(externalSchema);
        if (schemaFiles.isEmpty()) {
            logger.info("No schemas to validate against");
            return null;
        } else {
            List<TAR> reports = new ArrayList<>();
            for (FileInfo aSchemaFile: schemaFiles) {
                logger.info("Validating against ["+aSchemaFile.getFile().getName()+"]");
                TAR report = validateSchema(getInputStreamForValidation(), aSchemaFile.getFile());
                logReport(report, aSchemaFile.getFile().getName());
                reports.add(report);
                logger.info("Validated against ["+aSchemaFile.getFile().getName()+"]");
            }
            return Utils.mergeReports(reports);
        }
    }

    private TAR validateSchema(InputStream inputSource, File schemaFile) {
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler();
        // Resolve schema.
        SchemaFactory schemaFactory = XMLSchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setSchema(schema);
        Validator validator = schema.newValidator();
        validator.setErrorHandler(handler);
        TAR report;
        try {
            // Use a StreamSource rather than a DomSource below to get the line & column number of possible errors.
            StreamSource source = new StreamSource(inputSource);
            validator.validate(source);
            report = handler.createReport();
        } catch (Exception e) {
            logger.warn("Error while validating XML ["+e.getMessage()+"]");
            report = createFailureReport();
        }
        return report;
    }

    private TAR createEmptyReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.SUCCESS);
        return report;
    }

    private TAR createFailureReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.FAILURE);
        BAR error1 = new BAR();
        error1.setDescription("An error occurred due to a problem in given XML content.");
        error1.setLocation("XML:1:0");
        JAXBElement element1 = this.gitbTRObjectFactory.createTestAssertionGroupReportsTypeError(error1);
        report.getReports().getInfoOrWarningOrError().add(element1);
        return report;
    }

    private void completeReport(TAR report) {
        if (report != null) {
            if (report.getDate() == null) {
                report.setDate(Utils.getXMLGregorianCalendarDateTime());
            }
            if (report.getContext() == null) {
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

    private TAR validateAgainstSchematron() {
        List<TAR> reports = new ArrayList<>();
        List<FileInfo> schematronFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON);
        schematronFiles.addAll(externalSch);
        if (schematronFiles.isEmpty()) {
            logger.info("No schematrons to validate against");
            return null;
        } else {
            for (FileInfo aSchematronFile: schematronFiles) {
                logger.info("Validating against ["+aSchematronFile.getFile().getName()+"]");
                TAR report = validateSchematron(getInputStreamForValidation(), aSchematronFile.getFile());
                logReport(report, aSchematronFile.getFile().getName());
                reports.add(report);
                logger.info("Validated against ["+aSchematronFile.getFile().getName()+"]");
            }
            return Utils.mergeReports(reports);
        }
    }

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
            report.getReports().getInfoOrWarningOrError().forEach((item) -> {
                if (item.getValue() instanceof BAR) {
                    BAR reportItem = (BAR)item.getValue();
                    logOutput.append("\nDescription: ").append(reportItem.getDescription());
                }
            });
            logger.debug(logOutput.toString());
        }
    }


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
        return overallResult;
    }

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
                    if (response != null && response.getReport() != null) {
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

    private ValidateRequest preparePluginInput(File pluginTmpFolder) {
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID().toString()+".xml");
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
        return request;
    }

    private TAR validateSchematron(InputStream inputSource, File schematronFile) {
        Document schematronInput;
        SchematronOutputType svrlOutput;
        boolean convertXPathExpressions = false;
        String schematronFileName = schematronFile.getName().toLowerCase();
        if (schematronFileName.endsWith("xslt") || schematronFileName.endsWith("xsl")) {
            // Validate as XSLT.
            try {
                schematronInput = Utils.readXMLWithLineNumbers(inputSource);
                TransformerFactory factory = TransformerFactory.newInstance();
                factory.setURIResolver(getURIResolver(schematronFile));
                Transformer transformer = factory.newTransformer(new StreamSource(new FileInputStream(schematronFile)));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                transformer.setURIResolver(factory.getURIResolver());
                transformer.transform(new DOMSource(schematronInput), new StreamResult(bos));
                bos.flush();
                Unmarshaller jaxbUnmarshaller = SVRL_JAXB_CONTEXT.createUnmarshaller();
                JAXBElement<SchematronOutputType> root = jaxbUnmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(bos.toByteArray())), SchematronOutputType.class);
                svrlOutput = root.getValue();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            // Validate as raw schematron.
            convertXPathExpressions = true;
            ISchematronResource schematron = SchematronResourcePure.fromFile(schematronFile);
            if(schematron.isValidSchematron()) {
                try {
                    schematronInput = Utils.readXMLWithLineNumbers(inputSource);
                    svrlOutput = schematron.applySchematronValidationToSVRL(new DOMSource(schematronInput));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalStateException("Schematron file ["+schematronFile.getName()+"] is invalid");
            }
        }
        //handle validation report
        String xmlContent;
        try (InputStream in = getInputStreamForValidation()) {
            xmlContent = IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read input file", e);
        }
        SchematronReportHandler handler = new SchematronReportHandler(xmlContent, Utils.emptyDocument(), schematronInput, svrlOutput, convertXPathExpressions, domainConfig.isIncludeTestDefinition(), domainConfig.isReportsOrdered());
        return handler.createReport();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
