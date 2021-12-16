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

}
