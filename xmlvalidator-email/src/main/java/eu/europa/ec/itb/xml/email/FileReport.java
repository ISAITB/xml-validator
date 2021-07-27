package eu.europa.ec.itb.xml.email;

import com.gitb.tr.TAR;

/**
 * Class used to summarise a TAR validation report.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;
    private final boolean xmlReportSaved;
    private final boolean pdfReportSaved;

    /**
     * Constructor.
     *
     * @param fileName The filename to quote.
     * @param report The TAR report.
     */
    public FileReport(String fileName, TAR report) {
        this(fileName, report, true, true);
    }

    /**
     * Constructor.
     *
     * @param fileName The filename to quote.
     * @param report The TAR report.
     * @param xmlReportSaved True if the XML report was generated.
     * @param pdfReportSaved True if the PDF report was generated.
     */
    public FileReport(String fileName, TAR report, boolean xmlReportSaved, boolean pdfReportSaved) {
        this.fileName = fileName;
        this.report = report;
        this.xmlReportSaved = xmlReportSaved;
        this.pdfReportSaved = pdfReportSaved;
    }

    /**
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return The wrapped report.
     */
    public TAR getReport() {
        return report;
    }

    /**
     * @return The name of the XML report file.
     */
    public String getReportXmlFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.xml";
    }

    /**
     * @return The name of the PDF report file.
     */
    public String getReportPdfFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.pdf";
    }

    /**
     * Convert the provided report to a message for inclusion in email responses.
     *
     * @return The text.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation report for [").append(fileName).append("]:");
        sb.append("\n- Date: ").append(report.getDate());
        sb.append("\n- Result: ").append(report.getResult());
        sb.append("\n- Errors: ").append(report.getCounters().getNrOfErrors());
        sb.append("\n- Warnings: ").append(report.getCounters().getNrOfWarnings());
        sb.append("\n- Messages: ").append(report.getCounters().getNrOfAssertions());
        if (xmlReportSaved || pdfReportSaved) {
            sb.append("\n- Detailed report in: ");
            if (xmlReportSaved) {
                sb.append("XML [").append(getReportXmlFileName()).append("]");
            }
            if (pdfReportSaved) {
                if (xmlReportSaved) {
                    sb.append(" and ");
                }
                sb.append("PDF [").append(getReportPdfFileName()).append("]");
            }
        }
        return sb.toString();
    }
}
