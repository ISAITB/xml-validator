package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.types.ObjectType;
import com.gitb.types.SchemaType;
import com.gitb.validation.common.AbstractReportHandler;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLSuccessfulReport;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by simatosc on 26/02/2016.
 */
public class SchematronReportHandler extends AbstractReportHandler {
    private static Pattern ARRAY_PATTERN = Pattern.compile("\\[\\d+\\]");
    private static Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");
    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);
    private Document node;
    private SchematronOutputType svrlReport;
    private NamespaceContext namespaceContext;
    private Boolean hasDefaultNamespace;
    private boolean convertXPathExpressions;
    private boolean includeTest;
    private boolean reportsOrdered;

    public SchematronReportHandler(ObjectType xml, SchemaType sch, Document node, SchematronOutputType svrl, boolean convertXPathExpressions, boolean includeTest, boolean reportsOrdered) {
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
        this.convertXPathExpressions = convertXPathExpressions;
        this.includeTest = includeTest;
        this.reportsOrdered = reportsOrdered;
    }

    private NamespaceContext getNamespaceContext() {
        if (namespaceContext == null) {
            namespaceContext = new DocumentNamespaceContext(node, false);
        }
        return namespaceContext;
    }

    private <T extends AbstractSVRLMessage> TestResultType getErrorLevel(List<T> messages) {
        for (AbstractSVRLMessage item: messages) {
            if (item.getFlag() != null) {
                if (item.getFlag().getNumericLevel() == EErrorLevel.ERROR.getNumericLevel()
                    || item.getFlag().getNumericLevel() == EErrorLevel.FATAL_ERROR.getNumericLevel()) {
                    return TestResultType.FAILURE;
                }
            }
        }
        return TestResultType.SUCCESS;
    }

    public TAR createReport() {
        if (this.svrlReport != null) {
            List<SVRLFailedAssert> error = SVRLHelper.getAllFailedAssertions(this.svrlReport);
            this.report.setResult(TestResultType.SUCCESS);
            if (error.size() > 0) {
                this.report.setResult(getErrorLevel(error));
                List element = this.traverseSVRLMessages(error, true);
                this.report.getReports().getInfoOrWarningOrError().addAll(element);
            }
            List<SVRLSuccessfulReport> element = SVRLHelper.getAllSuccessfulReports(this.svrlReport);
            if (element.size() > 0) {
                if (this.report.getResult() == TestResultType.SUCCESS) {
                    this.report.setResult(getErrorLevel(element));
                }
                List successReports = this.traverseSVRLMessages(element, false);
                this.report.getReports().getInfoOrWarningOrError().addAll(successReports);
            }
        } else {
            this.report.setResult(TestResultType.FAILURE);
            BAR error1 = new BAR();
            error1.setDescription("An error occurred when generating Schematron output due to a problem in given XML content.");
            error1.setLocation(ValidationConstants.INPUT_XML+":1:0");
            JAXBElement element1 = this.objectFactory.createTestAssertionGroupReportsTypeError(error1);
            this.report.getReports().getInfoOrWarningOrError().add(element1);
        }
        if (reportsOrdered) {
            Collections.sort(this.report.getReports().getInfoOrWarningOrError(), new ReportItemComparator());
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
            error.setLocation(ValidationConstants.INPUT_XML+":" + this.getLineNumbeFromXPath(message.getLocation()) + ":0");
            if (message.getTest() != null && includeTest) {
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

    private XPathFactory getXPathFactory() {
        return new net.sf.saxon.xpath.XPathFactoryImpl();
    }

    private String getLineNumbeFromXPath(String xpathExpression) {
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression);
        XPath xPath = getXPathFactory().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        Node node;
        try {
            node = (Node)xPath.evaluate(xpathExpressionConverted, this.node, XPathConstants.NODE);
            return (String)node.getUserData("lineNumber");
        } catch (Exception e) {
            logger.error("Unable to locate line for expression ["+xpathExpression+"] ["+xpathExpressionConverted+"]: "+e.getMessage());
            return "0";
        }
    }

    private String convertToXPathExpression(String xpathExpression) {
        /*
        Schematron reports arrays as 0-based whereas xpath has 1-based arrays.
        This is used to increment each array index by one.
         */
        if (isXPathConversionNeeded()) {
            try {
                StringBuffer s = new StringBuffer();
                Matcher m = ARRAY_PATTERN.matcher(xpathExpression);
                while (m.find()) {
                    m.appendReplacement(s, "["+String.valueOf(1 + Integer.parseInt(m.group(0).substring(1, m.group(0).length()-1)))+"]");
                }
                m.appendTail(s);
                if (documentHasDefaultNamespace(node)) {
                    m = DEFAULTNS_PATTERN.matcher(s.toString());
                    s.delete(0, s.length());
                    while (m.find()) {
                        String match = m.group(0);
                        if (match.indexOf(':') == -1) {
                            match = "/"+DocumentNamespaceContext.DEFAULT_NS+":"+match.substring(1);
                        }
                        m.appendReplacement(s, match);
                    }
                    m.appendTail(s);
                }
                return s.toString();
            } catch (Exception e) {
                logger.warn("Failed to convert XPath expression.", e);
                return xpathExpression;
            }
        } else {
            return xpathExpression;
        }
    }

    private boolean isXPathConversionNeeded() {
        return convertXPathExpressions;
    }

    private boolean documentHasDefaultNamespace(Document node) {
        if (hasDefaultNamespace == null) {
            NamedNodeMap attributes = node.getFirstChild().getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                    hasDefaultNamespace = Boolean.TRUE;
                    break;
                }
            }
            if (hasDefaultNamespace == null) {
                hasDefaultNamespace = Boolean.FALSE;
            }
        }
        return hasDefaultNamespace.booleanValue();
    }

    private static class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

        @Override
        public int compare(JAXBElement<TestAssertionReportType> o1, JAXBElement<TestAssertionReportType> o2) {
            if (o1 == null && o1 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else {
                String name1 = o1.getName().getLocalPart();
                String name2 = o2.getName().getLocalPart();
                if (name1.equals(name2)) {
                    return 0;
                } else if ("error".equals(name1)) {
                    return -1;
                } else if ("error".equals(name2)) {
                    return 1;
                } else if ("warning".equals(name1)) {
                    return -1;
                } else if ("warning".equals(name2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }
}
