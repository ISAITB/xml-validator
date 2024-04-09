package eu.europa.ec.itb.xml.validation;

import com.gitb.tr.*;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helger.schematron.svrl.jaxb.Text;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to handle the generation of a Schematron validation report as a TAR report.
 */
public class SchematronReportHandler {

    private static final Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");
    private static final Logger logger = LoggerFactory.getLogger(SchematronReportHandler.class);
    private final Document node;
    private final SchematronOutputType svrlReport;
    private final boolean locationAsPath;
    private final LocalisationHelper localiser;
    private NamespaceContext namespaceContext;
    private XPathFactory xpathFactory;
    private Boolean hasDefaultNamespace;
    private final boolean convertXPathExpressions;
    private final boolean includeTest;
    private final boolean includeAssertionID;
    private final TAR report;
    private final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * Constructor.
     *
     * @param node The Schematron document.
     * @param svrl The raw Schamtron output.
     * @param convertXPathExpressions True if XPath expressions should be converted between SCH and XSLT.
     * @param includeTest True if the test per report item should be included.
     * @param includeAssertionID True if the assertion IDs per report item should be included.
     * @param locationAsPath True if report item locations should be XPath expressions. If not the line numbers will be
     *                       calculated and recorded instead.
     * @param localiser Helper class for translations.
     */
    public SchematronReportHandler(Document node, SchematronOutputType svrl, boolean convertXPathExpressions, boolean includeTest, boolean includeAssertionID, boolean locationAsPath, LocalisationHelper localiser) {
        this.node = node;
        this.svrlReport = svrl;
        report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        this.report.setName("Schematron Validation");
        this.report.setReports(new TestAssertionGroupReportsType());
        this.convertXPathExpressions = convertXPathExpressions;
        this.includeTest = includeTest;
        this.includeAssertionID = includeAssertionID;
        this.locationAsPath = locationAsPath;
        this.localiser = localiser;
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
            if (item.getFlag().getNumericLevel() == EErrorLevel.ERROR.getNumericLevel()
                || item.getFlag().getNumericLevel() == EErrorLevel.FATAL_ERROR.getNumericLevel()) {
                return TestResultType.FAILURE;
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
            var error = SVRLHelper.getAllFailedAssertions(this.svrlReport);
            this.report.setResult(TestResultType.SUCCESS);
            if (!error.isEmpty()) {
                this.report.setResult(getErrorLevel(error));
                var element = this.traverseSVRLMessages(error);
                this.report.getReports().getInfoOrWarningOrError().addAll(element);
            }
            var element = SVRLHelper.getAllSuccessfulReports(this.svrlReport);
            if (!element.isEmpty()) {
                if (this.report.getResult() == TestResultType.SUCCESS) {
                    this.report.setResult(getErrorLevel(element));
                }
                var successReports = this.traverseSVRLMessages(element);
                this.report.getReports().getInfoOrWarningOrError().addAll(successReports);
            }
        } else {
            this.report.setResult(TestResultType.FAILURE);
            BAR error1 = new BAR();
            error1.setDescription(localiser.localise("validator.label.exception.errorWithSchematronDueToProblemInXML"));
            error1.setLocation(ValidationConstants.INPUT_XML+":1:0");
            var element1 = this.objectFactory.createTestAssertionGroupReportsTypeError(error1);
            this.report.getReports().getInfoOrWarningOrError().add(element1);
        }
        return this.report;
    }

    /**
     * Convert the Schematron SVRL messages to report items to include in the TAR report.
     *
     * @param svrlMessages The SVRL messages.
     * @param <T> The SVRL message type.
     * @return The items for the TAR report.
     */
    private <T extends AbstractSVRLMessage> List<JAXBElement<TestAssertionReportType>> traverseSVRLMessages(List<T> svrlMessages) {
        ArrayList<JAXBElement<TestAssertionReportType>> reports = new ArrayList<>();
        JAXBElement<TestAssertionReportType> element;
        for(Iterator<T> var4 = svrlMessages.iterator(); var4.hasNext(); reports.add(element)) {
            AbstractSVRLMessage message = var4.next();
            BAR reportItem = new BAR();
            reportItem.setDescription(getMessageText(message));
            if (message.getLocation() != null && !message.getLocation().isBlank()) {
                if (locationAsPath) {
                    reportItem.setLocation(message.getLocation());
                } else {
                    reportItem.setLocation(ValidationConstants.INPUT_XML+":" + this.getLineNumberFromXPath(message.getLocation()) + ":0");
                }
            }
            if (message.getTest() != null && includeTest) {
                reportItem.setTest(message.getTest().trim());
            }
            if (message.getID() != null && includeAssertionID) {
                reportItem.setAssertionID(message.getID());
            }
            int level = message.getFlag().getNumericLevel();
            if (level == EErrorLevel.SUCCESS.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(reportItem);
            } else if (level == EErrorLevel.INFO.getNumericLevel()) {
                element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(reportItem);
            } else if (level == EErrorLevel.WARN.getNumericLevel()) {
                // Temporary fix until release ph-schematron v8.0.1 corrects this. Assertions with "CAUTION" role should be treated as information messages.
                if ("caution".equalsIgnoreCase(message.getRole())) {
                    element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(reportItem);
                } else {
                    element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(reportItem);
                }
            } else { // ERROR, FATAL_ERROR
                element = this.objectFactory.createTestAssertionGroupReportsTypeError(reportItem);
            }
        }
        return reports;
    }

    /**
     * Convert the provided content of a diagnostic element to a string.
     *
     * @param content The diagnostic content.
     * @return The string.
     */
    private String diagnosticContentAsString(Object content) {
        String messageText;
        if (content == null) {
            messageText = "";
        } else if (content instanceof String) {
            messageText = (String)content;
        } else if (content instanceof Text) {
            messageText = diagnosticContentAsString(((Text) content).getContent());
        } else if (content instanceof Iterable) {
            StringBuilder messageBuilder = new StringBuilder();
            for (var item: (Iterable<?>)content) {
                if (!messageBuilder.isEmpty()) {
                    messageBuilder.append(' ');
                }
                messageBuilder.append(diagnosticContentAsString(item));
            }
            messageText = messageBuilder.toString();
        } else {
            messageText = content.toString();
        }
        return messageText;
    }

    /**
     * Get the message to return for the provided SVRL message.
     *
     * @param svrlMessage The SVRL message.
     * @return The message for the report.
     */
    private String getMessageText(AbstractSVRLMessage svrlMessage) {
        StringBuilder message = null;
        if (svrlMessage != null) {
            var diagnostics = svrlMessage.getDiagnosticReferences();
            if (diagnostics.isNotEmpty()) {
                for (var diagnostic: diagnostics) {
                    if (localiser.getLocale().getLanguage().equalsIgnoreCase(diagnostic.getLang()) && diagnostic.hasContentEntries()) {
                        for (var content: diagnostic.getContent()) {
                            String messageText = diagnosticContentAsString(content);
                            if (message == null) {
                                message = new StringBuilder(messageText);
                            } else {
                                message.append(" ").append(messageText);
                            }
                        }
                        break;
                    }
                }
            }
        }
        if (message == null) {
            if (svrlMessage != null && svrlMessage.getText() != null) {
                message = new StringBuilder(svrlMessage.getText().trim());
            } else {
                message = new StringBuilder();
            }
        }
        return message.toString();
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
        String xpathExpressionConverted = convertToXPathExpression(xpathExpression);
        XPath xPath = getXPathFactory().newXPath();
        xPath.setNamespaceContext(getNamespaceContext());
        Node locatedNode;
        try {
            locatedNode = (Node)xPath.evaluate(xpathExpressionConverted, this.node, XPathConstants.NODE);
            if (locatedNode == null) {
                var expressionWithWildcards = convertToWildCardXPathExpression(xpathExpression);
                if (expressionWithWildcards.isPresent()) {
                    locatedNode = (Node)xPath.evaluate(expressionWithWildcards.get(), this.node, XPathConstants.NODE);
                }
            }
            if (locatedNode == null) {
                return "0";
            } else {
                return (String)locatedNode.getUserData("lineNumber");
            }
        } catch (Exception e) {
            logger.error("Unable to locate line for expression [{}] [{}]: {}", xpathExpression, xpathExpressionConverted, e.getMessage());
            return "0";
        }
    }

    /**
     * Adapt the provided XPath expression to change its path elements that don't have a prefix, to use a wildcard prefix.
     *
     * @param xpathExpression The XPath expression to process.
     * @return The adapted XPath expression or empty if no change was made to the original expression.
     */
    private Optional<String> convertToWildCardXPathExpression(String xpathExpression) {
        boolean changed = false;
        String expressionToReturn = xpathExpression;
        if (xpathExpression != null) {
            String[] pathParts = StringUtils.split(xpathExpression, '/');
            var builder = new StringBuilder();
            for (var pathPart: pathParts) {
                if (!builder.isEmpty()) {
                    builder.append('/');
                }
                if (pathPart.indexOf(':') == -1) {
                    changed = true;
                    builder.append("*:");
                }
                builder.append(pathPart);
            }
            expressionToReturn = builder.toString();
        }
        if (changed) {
            return Optional.of(expressionToReturn);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Convert the expression to an executable XPath expression by adding the default namespace if needed.
     *
     * @param xpathExpression The Schematron expression.
     * @return The expression to use via normal XPath lookup.
     */
    private String convertToXPathExpression(String xpathExpression) {
        if (convertXPathExpressions) {
            try {
                if (documentHasDefaultNamespace(node)) {
                    StringBuilder s = new StringBuilder(xpathExpression);
                    Matcher m = DEFAULTNS_PATTERN.matcher(s.toString());
                    s.delete(0, s.length());
                    while (m.find()) {
                        String match = m.group(0);
                        if (match.indexOf(':') == -1) {
                            match = "/"+DocumentNamespaceContext.DEFAULT_NS+":"+match.substring(1);
                        }
                        m.appendReplacement(s, match);
                    }
                    m.appendTail(s);
                    xpathExpression = s.toString();
                }
            } catch (Exception e) {
                logger.warn("Failed to convert XPath expression.", e);
            }
        }
        return xpathExpression;
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

}
