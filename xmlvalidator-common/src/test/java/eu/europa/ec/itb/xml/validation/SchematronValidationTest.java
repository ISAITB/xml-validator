package eu.europa.ec.itb.xml.validation;

import com.gitb.tr.BAR;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.sch.SchematronResourceSCH;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end regression test covering the Schematron performance fix for
 * <a href="https://github.com/ISAITB/xml-validator/issues/4">issue #4</a>: streaming the input into the XSLT-based
 * Schematron transformation (rather than a {@code DOMSource} wrapping the line-numbered DOM), and only resolving
 * that DOM lazily, on demand, to localise findings.
 * <p/>
 * These tests run real {@code .sch} rules through ph-schematron (compiling the bundled ISO skeleton, so no network
 * access is needed), to confirm the two input mechanisms produce the same validation outcome and, downstream, the
 * same {@link SchematronReportHandler} report - and that the line-numbered document is only ever resolved when
 * actually needed.
 */
class SchematronValidationTest {

    private File resource(String name) {
        var url = Thread.currentThread().getContextClassLoader().getResource("validation/schematron/" + name);
        assertNotNull(url, "Missing test resource: " + name);
        return new File(url.getFile());
    }

    /**
     * Run the Schematron validation the way it was done before the fix: the line-numbered DOM wrapped in a
     * {@link DOMSource}.
     */
    private SchematronOutputType applyUsingDomSource(File xmlFile, File schFile) throws Exception {
        ISchematronResource schematron = SchematronResourceSCH.fromFile(schFile);
        Document document;
        try (var in = new FileInputStream(xmlFile)) {
            document = eu.europa.ec.itb.validation.commons.Utils.readXMLWithLineNumbers(in);
        }
        var svrlDocument = schematron.applySchematronValidation(new DOMSource(document));
        assertNotNull(svrlDocument);
        return new SVRLMarshaller(false).read(svrlDocument);
    }

    /**
     * Run the Schematron validation the way it is done after the fix: the input streamed directly via the secure
     * SAX source, with no DOM built for the validation itself.
     */
    private SchematronOutputType applyUsingStreamedSource(File xmlFile, File schFile) throws Exception {
        ISchematronResource schematron = SchematronResourceSCH.fromFile(schFile);
        try (var in = new FileInputStream(xmlFile)) {
            var svrlDocument = schematron.applySchematronValidation(eu.europa.ec.itb.xml.util.Utils.secureSaxSource(in));
            assertNotNull(svrlDocument);
            return new SVRLMarshaller(false).read(svrlDocument);
        }
    }

    @Test
    void testStreamedAndDomValidationProduceEquivalentSVRL() throws Exception {
        File xmlFile = resource("sample.xml");
        File schFile = resource("quantity-check.sch");
        var domFailures = SVRLHelper.getAllFailedAssertions(applyUsingDomSource(xmlFile, schFile));
        var streamedFailures = SVRLHelper.getAllFailedAssertions(applyUsingStreamedSource(xmlFile, schFile));
        assertEquals(2, domFailures.size());
        assertEquals(domFailures.size(), streamedFailures.size());
        for (int i = 0; i < domFailures.size(); i++) {
            assertEquals(domFailures.get(i).getTest(), streamedFailures.get(i).getTest());
            assertEquals(domFailures.get(i).getLocation(), streamedFailures.get(i).getLocation());
            assertEquals(domFailures.get(i).getText(), streamedFailures.get(i).getText());
        }
    }

    @Test
    void testTarReportsAreIdenticalBetweenDomAndStreamedInput() throws Exception {
        File xmlFile = resource("sample.xml");
        File schFile = resource("quantity-check.sch");
        var localiser = new LocalisationHelper(Locale.ENGLISH);
        // Both reports are localised against the exact same document instance (built once, the "old" way) so that
        // the comparison isolates the effect of the input mechanism used for the Schematron transformation itself,
        // rather than of any difference in how the DOM was built.
        Document document;
        try (var in = new FileInputStream(xmlFile)) {
            document = eu.europa.ec.itb.validation.commons.Utils.readXMLWithLineNumbers(in);
        }
        TAR domReport = new SchematronReportHandler(() -> document, applyUsingDomSource(xmlFile, schFile), false, true, true, false, true, localiser).createReport();
        TAR streamedReport = new SchematronReportHandler(() -> document, applyUsingStreamedSource(xmlFile, schFile), false, true, true, false, true, localiser).createReport();

        assertEquals(TestResultType.FAILURE, domReport.getResult());
        assertEquals(domReport.getResult(), streamedReport.getResult());
        assertEquals(2, domReport.getReports().getInfoOrWarningOrError().size());
        assertEquals(domReport.getReports().getInfoOrWarningOrError().size(), streamedReport.getReports().getInfoOrWarningOrError().size());
        for (int i = 0; i < domReport.getReports().getInfoOrWarningOrError().size(); i++) {
            var domItem = (BAR) domReport.getReports().getInfoOrWarningOrError().get(i).getValue();
            var streamedItem = (BAR) streamedReport.getReports().getInfoOrWarningOrError().get(i).getValue();
            assertEquals(domItem.getLocation(), streamedItem.getLocation());
            assertEquals(domItem.getDescription(), streamedItem.getDescription());
            // Sanity: the fast resolver (or its XPath fallback) must have found an actual line, not defaulted to 0.
            assertNotNull(domItem.getLocation());
            assertFalse(domItem.getLocation().contains(":0:0"), "Unexpected fallback to line 0: " + domItem.getLocation());
        }
        // Line 2 (Quantity=0) is reported before line 3 (Quantity=-3), and their actual source lines differ.
        var firstLocation = ((BAR) domReport.getReports().getInfoOrWarningOrError().get(0).getValue()).getLocation();
        var secondLocation = ((BAR) domReport.getReports().getInfoOrWarningOrError().get(1).getValue()).getLocation();
        assertNotEquals(firstLocation, secondLocation);
    }

    @Test
    void testDocumentIsNotResolvedWhenLocationAsPathIsTrue() throws Exception {
        File xmlFile = resource("sample.xml");
        File schFile = resource("quantity-check.sch");
        var svrl = applyUsingStreamedSource(xmlFile, schFile);
        var localiser = new LocalisationHelper(Locale.ENGLISH);
        AtomicInteger callCount = new AtomicInteger();
        TAR report = new SchematronReportHandler(() -> {
            callCount.incrementAndGet();
            throw new AssertionError("Document should not be resolved when locationAsPath is true");
        }, svrl, false, true, true, true, true, localiser).createReport();
        assertEquals(TestResultType.FAILURE, report.getResult());
        assertEquals(2, report.getReports().getInfoOrWarningOrError().size());
        assertEquals(0, callCount.get());
    }

    @Test
    void testDocumentIsNotResolvedForACleanDocument() throws Exception {
        File xmlFile = resource("sample-clean.xml");
        File schFile = resource("quantity-check.sch");
        var svrl = applyUsingStreamedSource(xmlFile, schFile);
        var localiser = new LocalisationHelper(Locale.ENGLISH);
        AtomicInteger callCount = new AtomicInteger();
        TAR report = new SchematronReportHandler(() -> {
            callCount.incrementAndGet();
            throw new AssertionError("Document should not be resolved for a clean document");
        }, svrl, false, true, true, false, true, localiser).createReport();
        assertEquals(TestResultType.SUCCESS, report.getResult());
        assertEquals(0, callCount.get());
    }

    @Test
    void testDocumentIsResolvedOnlyOnceForMultipleFindings() throws Exception {
        File xmlFile = resource("sample.xml");
        File schFile = resource("quantity-check.sch");
        var svrl = applyUsingStreamedSource(xmlFile, schFile);
        var localiser = new LocalisationHelper(Locale.ENGLISH);
        AtomicInteger callCount = new AtomicInteger();
        Document document;
        try (var in = new FileInputStream(xmlFile)) {
            document = eu.europa.ec.itb.validation.commons.Utils.readXMLWithLineNumbers(in);
        }
        TAR report = new SchematronReportHandler(() -> {
            callCount.incrementAndGet();
            return document;
        }, svrl, false, true, true, false, true, localiser).createReport();
        assertEquals(2, report.getReports().getInfoOrWarningOrError().size());
        assertEquals(1, callCount.get());
    }
}
