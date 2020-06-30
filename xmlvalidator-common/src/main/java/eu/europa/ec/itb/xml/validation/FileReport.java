package eu.europa.ec.itb.xml.validation;

import com.gitb.tr.TAR;

/**
 * Created by simatosc on 12/08/2016.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;
    private final boolean xmlReportSaved;
    private final boolean pdfReportSaved;

    public FileReport(String fileName, TAR report) {
        this(fileName, report, true, true);
    }

    public FileReport(String fileName, TAR report, boolean xmlReportSaved, boolean pdfReportSaved) {
        this.fileName = fileName;
        this.report = report;
        this.xmlReportSaved = xmlReportSaved;
        this.pdfReportSaved = pdfReportSaved;
    }

    public String getFileName() {
        return fileName;
    }

    public TAR getReport() {
        return report;
    }

    public String getReportXmlFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.xml";
    }

    public String getReportPdfFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.pdf";
    }

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
