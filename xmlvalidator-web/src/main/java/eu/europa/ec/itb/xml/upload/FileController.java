package eu.europa.ec.itb.xml.upload;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller used for the manipulation of user inputs and produced reports.
 */
@RestController
public class FileController extends BaseFileController<FileManager, ApplicationConfig, DomainConfigCache> {

    /**
     * @see BaseFileController#getInputFileName(String)
     *
     * @param id The UUID.
     * @return The file name.
     */
    @Override
    public String getInputFileName(String id) {
        return fileManager.getInputFileName(id);
    }

    /**
     * @see BaseFileController#getReportFileNameXml(String)
     *
     * @param id The UUID.
     * @return The file name.
     */
    @Override
    public String getReportFileNameXml(String id) {
        return fileManager.getReportFileNameXml(id);
    }

    /**
     * @see BaseFileController#getReportFileNamePdf(String)
     *
     * @param id The UUID.
     * @return The file name.
     */
    @Override
    public String getReportFileNamePdf(String id) {
        return fileManager.getReportFileNamePdf(id);
    }

    /**
     * Get the XML report for the provided UUID.
     *
     * @param domain The domain identifier.
     * @param id The validation UUID.
     * @return The report as a file system resources.
     */
    public FileSystemResource getReportXml(String domain, String id) {
        return this.getReportXml(domain, id, null);
    }

    /**
     * Get the PDF report for the provided UUID.
     *
     * @param domain The domain identifier.
     * @param id The validation UUID.
     * @return The report as a file system resources.
     */
    public FileSystemResource getReportPdf(String domain, String id) {
        return this.getReportPdf(domain, id, null);
    }

}
