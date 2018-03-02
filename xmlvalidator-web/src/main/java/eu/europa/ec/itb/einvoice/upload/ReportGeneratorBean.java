package eu.europa.ec.itb.einvoice.upload;

import com.gitb.reports.ReportGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class ReportGeneratorBean {

    @Value("${validator.report.title:'Validation report'}")
    private String title;

    private ReportGenerator reportGenerator = new ReportGenerator();

    public void writeReport(File inFile, File outFile) {
        try (FileInputStream fis = new FileInputStream(inFile); FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(fis, title, fos);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate PDF report", e);
        }
    }

}
