package eu.europa.ec.itb.einvoice;

import org.apache.commons.configuration.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by simatosc on 04/03/2016.
 */
public class Configuration {

    private static Configuration instance;

    private File schematronFolder;
    private File schemaFile;
    private File reportFolder;
    private String inputFilePrefix;
    private long minimumCachedInputFileAge;
    private long minimumCachedReportFileAge;
    private String reportFilePrefix;

    public File getSchematronFolder() {
        return schematronFolder;
    }

    public File getSchemaFile() {
        return schemaFile;
    }

    public File getReportFolder() {
        return reportFolder;
    }

    public String getInputFilePrefix() {
        return inputFilePrefix;
    }

    public String getReportFilePrefix() {
        return reportFilePrefix;
    }

    public long getMinimumCachedInputFileAge() {
        return minimumCachedInputFileAge;
    }

    public long getMinimumCachedReportFileAge() {
        return minimumCachedReportFileAge;
    }

    public String getFaviconIconPath() {
        return null;
    }

    public static Configuration getInstance() {
        if (instance == null) {
            Configuration temp = new Configuration();
            CompositeConfiguration config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new EnvironmentConfiguration());
            String configPath = config.getString("config.path", "config.properties");
            try {
                config.addConfiguration(new PropertiesConfiguration(configPath));
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Unable to load configuration", e);
            }
            temp.schematronFolder = new File(config.getString("path.sch.folder"));
            if (!temp.schematronFolder.exists() || !temp.schematronFolder.isDirectory()) {
                throw new IllegalStateException("Schematron source folder ["+temp.schematronFolder.getAbsolutePath()+"] is not a valid directory.");
            }
            temp.schemaFile = new File(config.getString("path.xsd.file"));
            if (!temp.schemaFile.exists() || !temp.schemaFile.isFile()) {
                throw new IllegalStateException("Schema file ["+temp.schemaFile.getAbsolutePath()+"] is not valid.");
            }
            temp.reportFolder = new File(config.getString("path.report.folder", "./reports"));
            if (temp.reportFolder.exists() && temp.reportFolder.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(temp.reportFolder);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to clean up report folder", e);
                }
            }
            temp.reportFolder.mkdir();
            temp.minimumCachedInputFileAge = config.getLong("inputFile.minimumCacheTime", 600000L);
            temp.minimumCachedReportFileAge = config.getLong("reportFile.minimumCacheTime", 600000L);
            temp.inputFilePrefix = config.getString("inputFile.prefix", "ITB-");
            temp.reportFilePrefix = config.getString("reportFile.prefix", "TAR-");
            instance  = temp;
        }
        return instance;
    }

}
