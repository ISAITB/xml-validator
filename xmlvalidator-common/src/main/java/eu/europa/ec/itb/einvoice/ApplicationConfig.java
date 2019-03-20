package eu.europa.ec.itb.einvoice;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by simatosc on 21/03/2016.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    private Environment env;

    private boolean standalone = false;
    private String resourceRoot;
    private File reportFolder;
    private String inputFilePrefix = "ITB-";
    private long minimumCachedInputFileAge = 600000L;
    private long minimumCachedReportFileAge = 600000L;
    private String reportFilePrefix = "TAR-";
    private Set<String> acceptedMimeTypes;
    private Set<String> acceptedSchematronExtensions;
    private Set<String> domain;
    private Map<String, String> domainIdToDomainName = new HashMap<>();
    private Map<String, String> domainNameToDomainId = new HashMap<>();
    private String startupTimestamp;
    private String resourceUpdateTimestamp;

    public File getReportFolder() {
        return reportFolder;
    }

    public void setReportFolder(File reportFolder) {
        this.reportFolder = reportFolder;
    }

    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    public void setInputFilePrefix(String inputFilePrefix) {
        this.inputFilePrefix = inputFilePrefix;
    }

    public long getMinimumCachedInputFileAge() {
        return minimumCachedInputFileAge;
    }

    public void setMinimumCachedInputFileAge(long minimumCachedInputFileAge) {
        this.minimumCachedInputFileAge = minimumCachedInputFileAge;
    }

    public long getMinimumCachedReportFileAge() {
        return minimumCachedReportFileAge;
    }

    public void setMinimumCachedReportFileAge(long minimumCachedReportFileAge) {
        this.minimumCachedReportFileAge = minimumCachedReportFileAge;
    }

    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    public void setReportFilePrefix(String reportFilePrefix) {
        this.reportFilePrefix = reportFilePrefix;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    public Set<String> getAcceptedSchematronExtensions() {
        return acceptedSchematronExtensions;
    }

    public void setAcceptedSchematronExtensions(Set<String> acceptedSchematronExtensions) {
        this.acceptedSchematronExtensions = acceptedSchematronExtensions;
    }

    public Set<String> getDomain() {
        return domain;
    }

    public void setDomain(Set<String> domain) {
        this.domain = domain;
    }

    public String getStartupTimestamp() {
        return startupTimestamp;
    }

    public void setStartupTimestamp(String startupTimestamp) {
        this.startupTimestamp = startupTimestamp;
    }

    public String getResourceUpdateTimestamp() {
        return resourceUpdateTimestamp;
    }

    public void setResourceUpdateTimestamp(String resourceUpdateTimestamp) {
        this.resourceUpdateTimestamp = resourceUpdateTimestamp;
    }

    public Map<String, String> getDomainIdToDomainName() {
        return domainIdToDomainName;
    }

    public Map<String, String> getDomainNameToDomainId() {
        return domainNameToDomainId;
    }

    @PostConstruct
    public void init() {
        if (resourceRoot != null && Files.isDirectory(Paths.get(resourceRoot))) {
            // Setup domain.
            if (domain == null || domain.isEmpty()) {
                File[] directories = new File(resourceRoot).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+resourceRoot+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
            }
        } else {
            throw new IllegalStateException("Invalid resourceRoot configured ["+resourceRoot+"]. Ensure you specify the validator.resourceRoot property correctly.");
        }
        logger.info("Loaded validation domains: "+domain);
        // Load domain names.
        StringBuilder logMsg = new StringBuilder();
        for (String domainFolder: domain) {
            String domainName = StringUtils.defaultIfBlank(env.getProperty("validator.domainName."+domainFolder), domainFolder);
            this.domainIdToDomainName.put(domainFolder, domainName);
            this.domainNameToDomainId.put(domainName, domainFolder);
            logMsg.append('[').append(domainFolder).append("]=[").append(domainName).append("]");
        }
        logger.info("Loaded validation domain names: " + logMsg.toString());
        // Set startup times and resource update times.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (XXX)");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (XXX)");
        startupTimestamp = dtf.format(ZonedDateTime.now());
        resourceUpdateTimestamp = sdf.format(new Date(Paths.get(resourceRoot).toFile().lastModified()));
    }

}
