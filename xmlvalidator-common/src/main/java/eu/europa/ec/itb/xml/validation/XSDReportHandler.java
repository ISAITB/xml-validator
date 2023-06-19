package eu.europa.ec.itb.xml.validation;

import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.xml.bind.JAXBElement;

/**
 * Class to handle the generation of a XSD validation report as a TAR report.
 */
public class XSDReportHandler implements ErrorHandler {

    private static final String XML_ITEM_NAME = "XML";
    private static final Logger logger = LoggerFactory.getLogger(XSDReportHandler.class);

    private final TAR report;
    private final ObjectFactory objectFactory;

    /**
     * Constructor.
     */
    public XSDReportHandler() {
        objectFactory = new ObjectFactory();
        report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.SUCCESS);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
    }

    /**
     * @see ErrorHandler#warning(SAXParseException)
     *
     * @param exception The parse exception.
     * @throws SAXException If a parsing error occurs.
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        if (logger.isDebugEnabled()) {
            logger.debug("warning: <"+exception.getLineNumber() + "," +
                    exception.getColumnNumber() + ">" + exception.getMessage());
        }
        BAR warning = new BAR();
        warning.setDescription( exception.getMessage());
        warning.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeWarning(warning);
        report.getReports().getInfoOrWarningOrError().add(element);
    }

    /**
     * @see ErrorHandler#error(SAXParseException)
     *
     * @param exception The parse exception.
     * @throws SAXException If a parsing error occurs.
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        if (logger.isDebugEnabled()) {
            logger.debug("error: <" + exception.getLineNumber() + "," +
                    exception.getColumnNumber() + ">" + exception.getMessage());
        }
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
        error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }

    /**
     * @see ErrorHandler#fatalError(SAXParseException)
     *
     * @param exception The parse exception.
     * @throws SAXException If a parsing error occurs.
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        if (logger.isDebugEnabled()) {
            logger.debug("fatal error: <" + exception.getLineNumber() + "," +
                    exception.getColumnNumber() + ">" + exception.getMessage());
        }
        report.setResult(TestResultType.FAILURE);
        BAR error = new BAR();
        error.setDescription(exception.getMessage());
        error.setLocation(XML_ITEM_NAME+":"+exception.getLineNumber()+":"+exception.getColumnNumber());
        JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
        report.getReports().getInfoOrWarningOrError().add(element);
    }

    /**
     * Return the TAR report.
     *
     * @return The report.
     */
    public TAR createReport() {
        return report;
    }
}
