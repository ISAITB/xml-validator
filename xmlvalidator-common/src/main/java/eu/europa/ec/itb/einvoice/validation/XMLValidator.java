package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.apache.commons.io.FilenameUtils;
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

import javax.annotation.PostConstruct;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by simatosc on 26/02/2016.
 */
@Component
@Scope("prototype")
public class XMLValidator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);
    private static JAXBContext SVRL_JAXB_CONTEXT;
    private String validationType;
    protected ObjectFactory gitbTRObjectFactory = new ObjectFactory();

    static {
        try {
            SVRL_JAXB_CONTEXT = JAXBContext.newInstance(SchematronOutputType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB content for SchematronOutputType", e);
        }
    }

    @Autowired
    private ApplicationConfig config;

    private InputStream inputToValidate;
    private byte[] inputBytes;
    private ApplicationContext ctx;

    public XMLValidator(InputStream inputToValidate) {
        this(inputToValidate, null);
    }

    public XMLValidator(InputStream inputToValidate, String validationType) {
        this.inputToValidate = inputToValidate;
        this.validationType = validationType;
    }

    @PostConstruct
    public void init() {
        if (validationType == null) {
            validationType = config.getType().get(0);
        }
    }

    private InputStream getInputStreamForValidation() {
        if (inputBytes == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                inputBytes = StreamUtils.copyToByteArray(inputToValidate);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new ByteArrayInputStream(inputBytes);
    }

    private LSResourceResolver getXSDResolver() {
        return ctx.getBean(XSDFileResolver.class, validationType);
    }

    private javax.xml.transform.URIResolver getURIResolver(File schematronFile) {
        return ctx.getBean(URIResolver.class, validationType, schematronFile);
    }

    public TAR validateAgainstSchema() {
        File schemaFile = getSchemaFile();
        List<TAR> reports = new ArrayList<TAR>();
        List<File> schemaFiles = new ArrayList<>();
        if (schemaFile.isFile()) {
            // We are pointing to a single master schema file.
            schemaFiles.add(schemaFile);
        } else {
            // All schemas are to be processed.
            for (File aSchemaFile: schemaFile.listFiles()) {
                if (aSchemaFile.isFile()) {
                    schemaFiles.add(aSchemaFile);
                }
            }
        }
        for (File aSchemaFile: schemaFiles) {
            logger.info("Validating against ["+aSchemaFile.getName()+"]");
            TAR report = validateSchema(getInputStreamForValidation(), aSchemaFile);
            logReport(report, aSchemaFile.getName());
            reports.add(report);
            logger.info("Validated against ["+aSchemaFile.getName()+"]");
        }
        TAR report = mergeReports(reports.toArray(new TAR[reports.size()]));
        completeReport(report);
        return report;
    }

    public TAR validateSchema(InputStream inputSource, File schemaFile) {
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler();
        // Resolve schema.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(getXSDResolver());
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
        TAR report = null;
        try {
            // Use a StreamSource rather than a DomSource below to get the line & column number of possible errors.
            StreamSource source = new StreamSource(inputSource);
            validator.validate(source);
            report = handler.createReport();
        } catch (Exception e) {
            logger.warn("Error while validating XML ["+e.getMessage()+"]");
            report = createFailureReport();
        }
        completeReport(report);
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
                try {
                    report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
                } catch (DatatypeConfigurationException e) {
                    logger.error("Exception while creating XMLGregorianCalendar", e);
                }
            }
            if (report.getContext() == null) {
                report.setContext(new AnyContent());
                String inputXML = null;
                try {
                    inputXML = StreamUtils.copyToString(getInputStreamForValidation(), Charset.forName("UTF-8"));
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
                    if (itemName == "info") {
                        infos += 1;
                    } else if (itemName == "warning") {
                        warnings += 1;
                    } else if (itemName == "error") {
                        errors += 1;
                    }
                }
                report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
                report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
                report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
            }
        }
    }

    public TAR validateAgainstSchematron() {
        File schematronFile = getSchematronFile();
        List<TAR> reports = new ArrayList<TAR>();
        List<File> schematronFiles = new ArrayList<>();
        if (schematronFile.isFile()) {
            // We are pointing to a single master schematron file.
            schematronFiles.add(schematronFile);
        } else {
            // All schematrons are to be processed.
            for (File aSchematronFile: schematronFile.listFiles()) {
                if (aSchematronFile.isFile() && config.getAcceptedSchematronExtensions().contains(FilenameUtils.getExtension(aSchematronFile.getName().toLowerCase()))) {
                    schematronFiles.add(aSchematronFile);
                }
            }
        }
        if (schematronFiles.isEmpty()) {
            return null;
        } else {
            for (File aSchematronFile: schematronFiles) {
                logger.info("Validating against ["+aSchematronFile.getName()+"]");
                TAR report = validateSchematron(getInputStreamForValidation(), aSchematronFile);
                logReport(report, aSchematronFile.getName());
                reports.add(report);
                logger.info("Validated against ["+aSchematronFile.getName()+"]");
            }
            TAR report = mergeReports(reports.toArray(new TAR[reports.size()]));
            completeReport(report);
            return report;
        }
    }

    protected File getSchematronFile() {
        return new File(config.getResourceRoot()+config.getSchematronFile().get(validationType));
    }

    protected File getSchemaFile() {
        return new File(config.getResourceRoot()+config.getSchemaFile().get(validationType));
    }

    private void logReport(TAR report, String name) {
        if (logger.isDebugEnabled()) {
            StringBuilder logOutput = new StringBuilder();
            logOutput.append("["+name+"]\n Result: ").append(report.getResult());
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
        TAR schemaResult = validateAgainstSchema();
        if (schemaResult.getResult() != TestResultType.SUCCESS) {
            overallResult = schemaResult;
        } else {
            TAR schematronResult = validateAgainstSchematron();
            if (schematronResult != null) {
                overallResult = mergeReports(new TAR[] {schemaResult, schematronResult});
            } else {
                overallResult = mergeReports(new TAR[] {schemaResult});
            }
        }
        completeReport(overallResult);
        return overallResult;
    }

    private TAR mergeReports(TAR[] reports) {
        TAR mergedReport = reports[0];
        if (reports.length > 1) {
            for (int i=1; i < reports.length; i++) {
                TAR report = reports[i];
                if (report != null) {
                    if (report.getCounters() != null) {
                        if (mergedReport.getCounters() == null) {
                            mergedReport.setCounters(new ValidationCounters());
                            mergedReport.getCounters().setNrOfAssertions(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfWarnings(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfErrors(BigInteger.ZERO);
                        }
                        if (report.getCounters().getNrOfAssertions() != null) {
                            mergedReport.getCounters().setNrOfAssertions(mergedReport.getCounters().getNrOfAssertions().add(report.getCounters().getNrOfAssertions()));
                        }
                        if (report.getCounters().getNrOfWarnings() != null) {
                            mergedReport.getCounters().setNrOfWarnings(mergedReport.getCounters().getNrOfWarnings().add(report.getCounters().getNrOfWarnings()));
                        }
                        if (report.getCounters().getNrOfErrors() != null) {
                            mergedReport.getCounters().setNrOfErrors(mergedReport.getCounters().getNrOfErrors().add(report.getCounters().getNrOfErrors()));
                        }
                    }
                    if (report.getReports() != null) {
                        if (mergedReport.getReports() == null) {
                            mergedReport.setReports(new TestAssertionGroupReportsType());
                        }
                        mergedReport.getReports().getInfoOrWarningOrError().addAll(report.getReports().getInfoOrWarningOrError());
                    }
                    if (mergedReport.getResult() == null) {
                        mergedReport.setResult(TestResultType.UNDEFINED);
                    }
                    if (report.getResult() != null) {
                        if (mergedReport.getResult() == TestResultType.UNDEFINED || mergedReport.getResult() == TestResultType.SUCCESS) {
                            if (report.getResult() != TestResultType.UNDEFINED) {
                                mergedReport.setResult(report.getResult());
                            }
                        }
                    }
                    if (report.getContext() != null && mergedReport.getContext() == null) {
                        mergedReport.setContext(report.getContext());
                    }
                }
            }
        }
        return mergedReport;
    }

    public TAR validateSchematron(InputStream inputSource, File schematronFile) {
        Document schematronInput = null;
        SchematronOutputType svrlOutput = null;
        ISchematronResource schematron = null;
        boolean convertXPathExpressions = false;
        String schematronFileName = schematronFile.getName().toLowerCase();
        if (schematronFileName.endsWith("xslt") || schematronFileName.endsWith("xsl")) {
            // Validate as XSLT.
            schematron = SchematronResourceXSLT.fromFile(schematronFile);
            if(schematron.isValidSchematron()) {
                try {
                    schematronInput = XMLUtils.readXMLWithLineNumbers(inputSource);
                    Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new FileInputStream(schematronFile)));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    transformer.setURIResolver(getURIResolver(schematronFile));
                    transformer.transform(new DOMSource(schematronInput), new StreamResult(bos));
                    bos.flush();
                    Unmarshaller jaxbUnmarshaller = SVRL_JAXB_CONTEXT.createUnmarshaller();
                    JAXBElement<SchematronOutputType> root = jaxbUnmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(bos.toByteArray())), SchematronOutputType.class);
                    svrlOutput = root.getValue();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalStateException("Schematron file ["+schematronFile.getAbsolutePath()+"] is invalid");
            }
        } else {
            // Validate as raw schematron.
            convertXPathExpressions = true;
            schematron = SchematronResourcePure.fromFile(schematronFile);
            if(schematron.isValidSchematron()) {
                try {
                    schematronInput = XMLUtils.readXMLWithLineNumbers(inputSource);
                    svrlOutput = schematron.applySchematronValidationToSVRL(new DOMSource(schematronInput));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalStateException("Schematron file ["+schematronFile.getAbsolutePath()+"] is invalid");
            }
        }
        //handle validation report
        SchematronReportHandler handler = new SchematronReportHandler(new ObjectType(schematronInput), new SchemaType(), schematronInput, svrlOutput, convertXPathExpressions);
        return handler.createReport();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
