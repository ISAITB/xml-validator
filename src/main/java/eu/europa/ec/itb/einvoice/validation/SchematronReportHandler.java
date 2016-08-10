package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.validation.common.AbstractReportHandler;
import com.helger.commons.error.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLSuccessfulReport;
import eu.europa.ec.itb.einvoice.ws.ValidationService;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by simatosc on 26/02/2016.
 */
public class SchematronReportHandler extends AbstractReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);
    private Document node;
    private SchematronOutputType svrlReport;
    private NamespaceContext namespaceContext;

    public SchematronReportHandler(ObjectType xml, SchemaType sch, Document node, SchematronOutputType svrl) {
        this.node = node;
        this.svrlReport = svrl;
        this.report.setName("Schematron Validation");
        this.report.setReports(new TestAssertionGroupReportsType());
        AnyContent attachment = new AnyContent();
        attachment.setType("map");
        AnyContent xmlAttachment = new AnyContent();
        xmlAttachment.setName("XML");
        xmlAttachment.setType("object");
        xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        xmlAttachment.setValue(new String(xml.serializeByDefaultEncoding()));
        attachment.getItem().add(xmlAttachment);
        AnyContent schemaAttachment = new AnyContent();
        schemaAttachment.setName("SCH");
        schemaAttachment.setType("schema");
        schemaAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        schemaAttachment.setValue(new String(sch.serializeByDefaultEncoding()));
        attachment.getItem().add(schemaAttachment);
        this.report.setContext(attachment);
    }

    private NamespaceContext getNamespaceContext() {
        if (namespaceContext == null) {
            namespaceContext = new DocumentNamespaceContext(node, false);
        }
        return namespaceContext;
    }

    private TestResultType getErrorLevel(List<SVRLFailedAssert> error) {
        for (SVRLFailedAssert errorItem: error) {
            if (errorItem.getFlag() != null) {
                if (errorItem.getFlag().getNumericLevel() == EErrorLevel.ERROR.getNumericLevel()
                    || errorItem.getFlag().getNumericLevel() == EErrorLevel.FATAL_ERROR.getNumericLevel()) {
                    return TestResultType.FAILURE;
                }
            }
        }
        return TestResultType.SUCCESS;
    }

    public TAR createReport() {
        if (this.svrlReport != null) {
            List<SVRLFailedAssert> error = SVRLHelper.getAllFailedAssertions(this.svrlReport);
            if (error.size() > 0) {
                this.report.setResult(getErrorLevel(error));
                List element = this.traverseSVRLMessages(error, true);
                this.report.getReports().getInfoOrWarningOrError().addAll(element);
            }
            List<SVRLSuccessfulReport> element = SVRLHelper.getAllSuccessfulReports(this.svrlReport);
            if (element.size() > 0) {
                List successReports = this.traverseSVRLMessages(element, false);
                this.report.getReports().getInfoOrWarningOrError().addAll(successReports);
            }
        } else {
            this.report.setResult(TestResultType.FAILURE);
            BAR error1 = new BAR();
            error1.setDescription("An error occurred when generating Schematron output due to a problem in given XML content.");
            error1.setLocation(ValidationService.INPUT_XML+":1:0");
            JAXBElement element1 = this.objectFactory.createTestAssertionGroupReportsTypeError(error1);
            this.report.getReports().getInfoOrWarningOrError().add(element1);
        }
        return this.report;
    }

    private <T extends AbstractSVRLMessage> List<JAXBElement<TestAssertionReportType>> traverseSVRLMessages(List<T> svrlMessages, boolean failure) {
        ArrayList reports = new ArrayList();
        JAXBElement element;
        for(Iterator var4 = svrlMessages.iterator(); var4.hasNext(); reports.add(element)) {
            AbstractSVRLMessage message = (AbstractSVRLMessage)var4.next();
            BAR error = new BAR();
            if (message.getText() != null) {
                error.setDescription(message.getText().trim());
            }
            error.setLocation(ValidationService.INPUT_XML+":" + this.getLineNumbeFromXPath(message.getLocation()) + ":0");
            if (message.getTest() != null) {
                error.setTest(message.getTest().trim());
            }
            int level = message.getFlag().getNumericLevel();
            if (level == EErrorLevel.SUCCESS.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
            } else if (level == EErrorLevel.INFO.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
            } else if (level == EErrorLevel.WARN.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(error);
            } else { // ERROR, FATAL_ERROR
                element = this.objectFactory.createTestAssertionGroupReportsTypeError(error);
            }
        }
        return reports;
    }

    private String getLineNumbeFromXPath(String xpathExpression) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        try {
            Node node = (Node)xPath.evaluate(xpathExpression, this.node, XPathConstants.NODE);
            return (String)node.getUserData("lineNumber");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "0";
        }
    }
}
