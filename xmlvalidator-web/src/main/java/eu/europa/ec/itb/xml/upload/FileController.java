package eu.europa.ec.itb.xml.upload;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by simatosc on 08/03/2016.
 */
@RestController
public class FileController extends BaseFileController<FileManager, ApplicationConfig, DomainConfigCache> {

    @Override
    public String getInputFileName(String id) {
        return fileManager.getInputFileName(id);
    }

    @Override
    public String getReportFileNameXml(String id) {
        return fileManager.getReportFileNameXml(id);
    }

    @Override
    public String getReportFileNamePdf(String id) {
        return fileManager.getReportFileNamePdf(id);
    }

    public FileSystemResource getReportXml(String domain, String id) {
        return this.getReportXml(domain, id, null);
    }

    public FileSystemResource getReportPdf(String domain, String id) {
        return this.getReportPdf(domain, id, null);
    }

}
