package eu.europa.ec.itb.einvoice.validation;

import com.gitb.tr.*;
import com.gitb.utils.XMLDateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Created by simatosc on 07/03/2016.
 */
public class XSDReportHandler implements ErrorHandler {

    private static final String XML_ITEM_NAME = "XML";
    private static Logger logger = LoggerFactory.getLogger(XSDReportHandler.class);

    private TAR report;
    private ObjectFactory objectFactory;

    public XSDReportHandler() {
        objectFactory = new ObjectFactory();
        report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

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

    public TAR createReport() {
        return report;
    }
}
