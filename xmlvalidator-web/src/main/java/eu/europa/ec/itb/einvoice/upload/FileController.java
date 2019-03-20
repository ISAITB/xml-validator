package eu.europa.ec.itb.einvoice.upload;

import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.DomainConfigCache;
import eu.europa.ec.itb.einvoice.ValidatorChannel;
import eu.europa.ec.itb.einvoice.util.FileManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by simatosc on 08/03/2016.
 */
@RestController
public class FileController {

    @Autowired
    ApplicationConfig config;
    @Autowired
    FileManager fileManager;
    @Autowired
    ReportGeneratorBean reportGenerator;
    @Autowired
    DomainConfigCache domainConfigCache;

    @RequestMapping(value = "/{domain}/xml/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getXML(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }

    @RequestMapping(value = "/{domain}/report/{id}/xml", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getReportXml(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileNameXml(id));
        if (reportFile.exists() && reportFile.isFile()) {
            if (response != null) {
                response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".xml");
            }
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }

    @RequestMapping(value = "/{domain}/report/{id}/pdf", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportPdf(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileNamePdf(id));
        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            File reportFileXml = new File(config.getReportFolder(), fileManager.getReportFileNameXml(id));
            if (reportFileXml.exists() && reportFileXml.isFile()) {
                reportGenerator.writeReport(domainConfig, reportFileXml, reportFile);
            } else {
                throw new NotFoundException();
            }
        }
        if (response != null) {
            response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".pdf");
        }
        return new FileSystemResource(reportFile);
    }

    public FileSystemResource getReportXml(String domain, String id) {
        return this.getReportXml(domain, id, null);
    }

    public FileSystemResource getReportPdf(String domain, String id) {
        return this.getReportPdf(domain, id, null);
    }

    @RequestMapping(value = "/{domain}/report/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteReport(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileNameXml(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
        reportFile = new File(config.getReportFolder(), fileManager.getReportFileNamePdf(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }

    @RequestMapping(value = "/{domain}/xml/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteXML(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
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

}
