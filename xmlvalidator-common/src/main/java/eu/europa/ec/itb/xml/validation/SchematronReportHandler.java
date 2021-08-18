package eu.europa.ec.itb.xml.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLSuccessfulReport;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import eu.europa.ec.itb.validation.commons.Utils;
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
 * Class to handle the generation of a Schematron validation report as a TAR report.
 */
public class SchematronReportHandler {

    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\d+\\]");
    private static final Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");
    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);
    private final Document node;
    private final SchematronOutputType svrlReport;
    private final boolean locationAsPath;
    private NamespaceContext namespaceContext;
    private XPathFactory xpathFactory;
    private Boolean hasDefaultNamespace;
    private final boolean convertXPathExpressions;
    private final boolean includeTest;
    private final boolean reportsOrdered;
    private final TAR report;
    private final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * Constructor.
     *
     * @param node The Schematron document.
     * @param svrl The raw Schamtron output.
     * @param convertXPathExpressions True if XPath expressions should be converted between SCH and XSLT.
     * @param includeTest True if the test per report item should be included.
     * @param reportsOrdered True is reports should be ordered based on severity.
     * @param locationAsPath True if report item locations should be XPath expressions. If not the line numbers will be
     *                       calculated and recorded instead.
     */
    public SchematronReportHandler(Document node, SchematronOutputType svrl, boolean convertXPathExpressions, boolean includeTest, boolean reportsOrdered, boolean locationAsPath) {
        this.node = node;
        this.svrlReport = svrl;
        report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        this.report.setName("Schematron Validation");
        this.report.setReports(new TestAssertionGroupReportsType());
        this.convertXPathExpressions = convertXPathExpressions;
        this.includeTest = includeTest;
        this.reportsOrdered = reportsOrdered;
        this.locationAsPath = locationAsPath;
    }

    /**
     * Get or initialise the namespace content to be used for namespace resolution during parsing.
     *
     * @return The context.
     */
    private NamespaceContext getNamespaceContext() {
        if (namespaceContext == null) {
            namespaceContext = new DocumentNamespaceContext(node, false);
        }
        return namespaceContext;
    }

    /**
     * Return the overall report result from the recorded messages.
     *
     * @param messages The messages to check.
     * @param <T> The class of report message.
     * @return The overall validation result.
     */
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

    /**
     * Create the TAR report based on the provided input.
     *
     * @return The TAR report.
     */
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

    /**
     * Convert the Schematron SVRL messages to report items to include in the TAR report.
     *
     * @param svrlMessages The SVRL messages.
     * @param failure Whether the validation is overall a failure.
     * @param <T> The SVRL message type.
     * @return The items for the TAR report.
     */
    private <T extends AbstractSVRLMessage> List<JAXBElement<TestAssertionReportType>> traverseSVRLMessages(List<T> svrlMessages, boolean failure) {
        ArrayList<JAXBElement<TestAssertionReportType>> reports = new ArrayList<>();
        JAXBElement<TestAssertionReportType> element;
        for(Iterator<T> var4 = svrlMessages.iterator(); var4.hasNext(); reports.add(element)) {
            AbstractSVRLMessage message = var4.next();
            BAR error = new BAR();
            if (message.getText() != null) {
                error.setDescription(message.getText().trim());
            }
            if (message.getLocation() != null && !message.getLocation().isBlank()) {
                if (locationAsPath) {
                    error.setLocation(convertToXPathExpression(message.getLocation(), false));
                } else {
                    error.setLocation(ValidationConstants.INPUT_XML+":" + this.getLineNumberFromXPath(message.getLocation()) + ":0");
                }
            }
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

    /**
     * Construct the specific XPath factory to use (force it to be a Saxon implementation).
     *
     * @return The factory.
     */
    private XPathFactory getXPathFactory() {
        if (xpathFactory == null) {
            xpathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
        }
        return xpathFactory;
    }

    /**
     * Extract the line number corresponding to the input XML node referred to by this XPath expression.
     *
     * @param xpathExpression The expression.
     * @return The line number.
     */
    private String getLineNumberFromXPath(String xpathExpression) {
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression, true);
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

    /**
     * Convert the expression to ensure it is correct. This is because Schematron reports arrays as 0-based whereas xpath
     * has 1-based arrays. This is used to increment each array index by one.
     *
     * @param xpathExpression The Schematron expression.
     * @param addDefaultNamespace True if the defaull namespace prefix should be added to elements.
     * @return The expression to use via normal XPath lookup.
     */
    private String convertToXPathExpression(String xpathExpression, boolean addDefaultNamespace) {
        if (isXPathConversionNeeded()) {
            try {
                StringBuilder s = new StringBuilder();
                Matcher m = ARRAY_PATTERN.matcher(xpathExpression);
                while (m.find()) {
                    m.appendReplacement(s, "["+ (1 + Integer.parseInt(m.group(0).substring(1, m.group(0).length() - 1))) +"]");
                }
                m.appendTail(s);
                if (addDefaultNamespace && documentHasDefaultNamespace(node)) {
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

    /**
     * Check to see if XPath expressions need to be converted (which is the case for raw Schematron output).
     *
     * @return True if yes.
     */
    private boolean isXPathConversionNeeded() {
        return convertXPathExpressions;
    }

    /**
     * Check to see if the provided document has a default namespace definition.
     *
     * @param node The document to check.
     * @return The check result.
     */
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
        return hasDefaultNamespace;
    }

    /**
     * Comparator for TAR report items to allow their sorting.
     */
    private static class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

        /**
         * @see Comparator#compare(Object, Object)
         *
         * Errors come before warnings which come before information messages.
         *
         * @param o1 Object 1.
         * @param o2 Object 2.
         * @return The compare result.
         */
        @Override
        public int compare(JAXBElement<TestAssertionReportType> o1, JAXBElement<TestAssertionReportType> o2) {
            if (o1 == null && o2 == null) {
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
