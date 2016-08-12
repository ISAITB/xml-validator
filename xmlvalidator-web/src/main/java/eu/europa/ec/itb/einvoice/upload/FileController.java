package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.util.FileManager;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.UUID;

/**
 * Created by simatosc on 08/03/2016.
 */
@RestController
public class FileController {

    private static Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    ApplicationConfig config;
    @Autowired
    FileManager fileManager;

    @RequestMapping(value = "/xml/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getXML(@PathVariable String id) {
        File reportFile = new File(config.getReportFolder(), fileManager.getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            return new FileSystemResource(reportFile);
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping(value = "/report/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReport(@PathVariable String id, HttpServletResponse response) {
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            if (response != null) {
                response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".xml");
            }
            return new FileSystemResource(reportFile);
        } else {
            throw new ResourceNotFoundException();
        }
    }

    public FileSystemResource getReport(String id) {
        return this.getReport(id, null);
    }

    @RequestMapping(value = "/report/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteReport(@PathVariable String id) {
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

    @RequestMapping(value = "/xml/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteXML(@PathVariable String id) {
        File reportFile = new File(config.getReportFolder(), fileManager.getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

    @Scheduled(fixedDelayString = "${validator.cleanupPollingRate}")
    public void cleanUpFiles() {
        long currentMillis = System.currentTimeMillis();
        File reportFolder = config.getReportFolder();
        if (reportFolder != null) {
            File[] files = reportFolder.listFiles();
            if (files != null) {
                for (File file: files) {
                    if (!handleReportFile(file, config.getInputFilePrefix(), currentMillis, config.getMinimumCachedInputFileAge())) {
                        handleReportFile(file, config.getReportFilePrefix(), currentMillis, config.getMinimumCachedReportFileAge());
                    }
                }
            }
        }
    }

    private boolean handleReportFile(File file, String prefixToConsider, long currentTime, long minimumCacheTime) {
        boolean handled = false;
        if (file.getName().startsWith(prefixToConsider)) {
            handled = true;
            if (currentTime - file.lastModified() > minimumCacheTime) {
                FileUtils.deleteQuietly(file);
            }
        }
        return handled;
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
    }

}
