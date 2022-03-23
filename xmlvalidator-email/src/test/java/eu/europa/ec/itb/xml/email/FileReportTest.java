package eu.europa.ec.itb.xml.email;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

class FileReportTest {

    @Test
    void testCreationAndAccess() throws DatatypeConfigurationException {
        var fileName = "name1.xml";
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setCounters(new ValidationCounters());
        report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        report.getCounters().setNrOfErrors(BigInteger.ZERO);
        report.getCounters().setNrOfWarnings(BigInteger.ZERO);
        report.getCounters().setNrOfAssertions(BigInteger.ZERO);
        var fileReport = new FileReport(fileName, report, true, true);
        assertEquals(fileName, fileReport.getFileName());
        assertNotNull(fileReport.getReport());
        assertEquals(TestResultType.SUCCESS, fileReport.getReport().getResult());
        assertEquals("name1.report.xml", fileReport.getReportXmlFileName());
        assertEquals("name1.report.pdf", fileReport.getReportPdfFileName());
        var text = fileReport.toString();
        assertTrue(text.contains("Validation report for ["+fileName+"]"));
        assertTrue(text.contains("Date: "+report.getDate()));
        assertTrue(text.contains("Result: "+report.getResult()));
        assertTrue(text.contains("Errors: 0"));
        assertTrue(text.contains("Warnings: 0"));
        assertTrue(text.contains("Messages: 0"));
        assertTrue(text.contains("XML [name1.report.xml]"));
        assertTrue(text.contains("PDF [name1.report.pdf]"));
    }

}
