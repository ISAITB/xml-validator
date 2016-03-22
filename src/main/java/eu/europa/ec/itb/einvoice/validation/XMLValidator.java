package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.AnyContent;
import com.gitb.tr.*;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.utils.XMLDateTimeUtils;
import com.gitb.utils.XMLUtils;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Node;

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
public class XMLValidator {

    private static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);
    private static JAXBContext SVRL_JAXB_CONTEXT;
    protected ObjectFactory gitbTRObjectFactory = new ObjectFactory();

    static {
        try {
            SVRL_JAXB_CONTEXT = JAXBContext.newInstance(SchematronOutputType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB content for SchematronOutputType", e);
        }
    }

    @Autowired
    ApplicationConfig config;

    @Autowired
    XSDResolver xsdResolver;

    private InputStream inputToValidate;
    private byte[] inputBytes;

    public XMLValidator(InputStream inputToValidate) {
        this.inputToValidate = inputToValidate;
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

    public TAR validateAgainstSchema() {
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler();
        // Resolve schema.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(xsdResolver);
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new StreamSource(new FileInputStream(getSchemaFile())));
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
            StreamSource source = new StreamSource(getInputStreamForValidation());
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
        File schematronFolder = getSchematronFolder();
        List<TAR> reports = new ArrayList<TAR>();
        for (File xslFile: schematronFolder.listFiles()) {
            logger.info("Validating against ["+xslFile.getName()+"]");
            TAR report = validateSchematron(getInputStreamForValidation(), xslFile);
            logReport(report, xslFile.getName());
            reports.add(report);
            logger.info("Validated against ["+xslFile.getName()+"]");
        }
        TAR report = mergeReports(reports.toArray(new TAR[reports.size()]));
        completeReport(report);
        return report;
    }

    protected File getSchematronFolder() {
        return config.getSchematronFolder();
    }

    protected File getSchemaFile() {
        return config.getSchemaFile();
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
        TAR overallResult = null;
        TAR schemaResult = validateAgainstSchema();
        if (schemaResult.getResult() != TestResultType.SUCCESS) {
            overallResult = schemaResult;
        } else {
            TAR schematronResult = validateAgainstSchematron();
            overallResult = mergeReports(new TAR[] {schemaResult, schematronResult});
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
        final ISchematronResource schematron = SchematronResourceXSLT.fromFile(schematronFile);
        Node schematronInput = null;
        SchematronOutputType svrlOutput = null;
        if(schematron.isValidSchematron()) {
            try {
                schematronInput = XMLUtils.readXMLWithLineNumbers(inputSource);
                Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new FileInputStream(schematronFile)));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                transformer.transform(new DOMSource(schematronInput), new StreamResult(bos));
                bos.flush();
                Unmarshaller jaxbUnmarshaller = SVRL_JAXB_CONTEXT.createUnmarshaller();
                JAXBElement<SchematronOutputType> root = jaxbUnmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(bos.toByteArray())), SchematronOutputType.class);
                svrlOutput = root.getValue();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        //handle validation report
        SchematronReportHandler handler = new SchematronReportHandler(new ObjectType(schematronInput), new SchemaType(), schematronInput, svrlOutput);
        return handler.createReport();
    }


}
