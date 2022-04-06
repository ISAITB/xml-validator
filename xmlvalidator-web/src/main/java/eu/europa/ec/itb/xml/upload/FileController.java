package eu.europa.ec.itb.xml.upload;

import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
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
     * @see BaseFileController#getReportFileNameXml(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNameXml(String id, boolean aggregate) {
        return fileManager.getReportFileNameXml(id, aggregate);
    }

    /**
     * @see BaseFileController#getReportFileNamePdf(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNamePdf(String id, boolean aggregate) {
        return fileManager.getReportFileNamePdf(id, aggregate);
    }

    /**
     * @see BaseFileController#getReportFileNameCsv(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNameCsv(String id, boolean aggregate) {
        return fileManager.getReportFileNameCsv(id, aggregate);
    }

}
