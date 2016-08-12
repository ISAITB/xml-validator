package eu.europa.ec.itb.einvoice.validation;

import com.gitb.tr.TAR;

/**
 * Created by simatosc on 12/08/2016.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;

    public FileReport(String fileName, TAR report) {
        this.fileName = fileName;
        this.report = report;
    }

    public String getFileName() {
        return fileName;
    }

    public TAR getReport() {
        return report;
    }

    public String getReportFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.xml";
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
        sb.append("\n- Detailed report in: [").append(getReportFileName()).append(']');
        return sb.toString();
    }
}
