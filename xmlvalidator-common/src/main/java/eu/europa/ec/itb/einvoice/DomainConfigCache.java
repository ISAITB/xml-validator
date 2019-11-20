package eu.europa.ec.itb.einvoice;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DomainConfigCache {

    private static Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    private ApplicationConfig appConfig;
    private ConcurrentHashMap<String, DomainConfig> domainConfigs = new ConcurrentHashMap<>();

    private ExtensionFilter propertyFilter = new ExtensionFilter(".properties");
    private DomainConfig undefinedDomainConfig = new DomainConfig(false);

    @PostConstruct
    public void init() {
        getAllDomainConfigurations();
    }

    public DomainConfig[] getAllDomainConfigurations() {
        List<DomainConfig> configs = new ArrayList<>();
        for (String domain: appConfig.getDomain()) {
            DomainConfig domainConfig = getConfigForDomain(domain);
            if (domainConfig != null) {
                configs.add(domainConfig);
            }
        }
        return configs.toArray(new DomainConfig[0]);
    }

    public DomainConfig getConfigForDomainName(String domainName) {
        DomainConfig config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            logger.warn("Invalid domain name ["+domainName+"].");
        }
        return config;
    }

    private DomainConfig getConfigForDomain(String domain) {
        DomainConfig domainConfig = domainConfigs.get(domain);
        if (domainConfig == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfig = undefinedDomainConfig;
            } else {
                CompositeConfiguration config = new CompositeConfiguration();
                for (String file: files) {
                    Parameters params = new Parameters();
                    FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                            new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                                    .configure(params.properties().setFile(Paths.get(appConfig.getResourceRoot(), domain, file).toFile()));
                    try {
                        config.addConfiguration(builder.getConfiguration());
                    } catch (ConfigurationException e) {
                        throw new IllegalStateException("Unable to load property file ["+file+"]", e);
                    }
                }
                domainConfig = new DomainConfig();
                domainConfig.setDomain(domain);
                domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                domainConfig.setUploadTitle(config.getString("validator.uploadTitle", "Validator"));
                domainConfig.setType(Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList()));
                domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", ValidatorChannel.FORM.getName()+","+ValidatorChannel.WEB_SERVICE.getName()), ',')).map(String::trim).map(ValidatorChannel::byName).collect(Collectors.toSet()));
                domainConfig.setReportTitle(config.getString("validator.reportTitle", "Validation report"));
                domainConfig.setWebServiceId(config.getString("validator.webServiceId", "ValidatorService"));
                domainConfig.setMailFrom(config.getString("validator.mailFrom", null));
                domainConfig.setMailAuthEnable(config.getBoolean("validator.mailAuthEnable", false));
                domainConfig.setMailAuthUsername(config.getString("validator.mailAuthUsername", null));
                domainConfig.setMailAuthPassword(config.getString("validator.mailAuthPassword", null));
                domainConfig.setMailOutboundHost(config.getString("validator.mailOutboundHost", null));
                domainConfig.setMailOutboundPort(config.getInt("validator.mailOutboundPort", -1));
                domainConfig.setMailOutboundSSLEnable(config.getBoolean("validator.mailOutboundSSLEnable", false));
                domainConfig.setMailInboundHost(config.getString("validator.mailInboundHost", null));
                domainConfig.setMailInboundPort(config.getInt("validator.mailInboundPort", -1));
                domainConfig.setMailInboundSSLEnable(config.getBoolean("validator.mailInboundSSLEnable", false));
                domainConfig.setMailInboundFolder(config.getString("validator.mailInboundFolder", "INBOX"));
                domainConfig.setTypeLabel(parseMap("validator.typeLabel", config, domainConfig.getType()));
                domainConfig.setWebServiceDescription(parseMap("validator.webServiceDescription", config, Arrays.asList("xml", "type")));
                domainConfig.setSchemaFile(parseMap("validator.schemaFile", config, domainConfig.getType()));
                domainConfig.setSchematronFile(parseMap("validator.schematronFile", config, domainConfig.getType()));
                domainConfig.setIncludeTestDefinition(config.getBoolean("validator.includeTestDefinition", true));
                domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
                domainConfig.setShowAbout(config.getBoolean("validator.showAbout", true));
                domainConfig.setSupportMinimalUserInterface(config.getBoolean("validator.supportMinimalUserInterface", false));
                domainConfig.setHtmlBanner(config.getString("validator.bannerHtml", ""));
                domainConfig.setHtmlFooter(config.getString("validator.footerHtml", ""));
                setLabels(domainConfig, config);
                logger.info("Loaded configuration for domain ["+domain+"]");
            }
            domainConfigs.put(domain, domainConfig);
        }
        if (domainConfig.isDefined()) {
            return domainConfig;
        } else {
            return null;
        }
    }

    private void setLabels(DomainConfig domainConfig, CompositeConfiguration config) {
        // If the required labels ever increase the following code should be transformed into proper resource management.
        domainConfig.getLabel().setResultSectionTitle(config.getString("validator.label.resultSectionTitle", "Validation result"));
        domainConfig.getLabel().setFileInputLabel(config.getString("validator.label.fileInputLabel", "Content to validate"));
        domainConfig.getLabel().setFileInputPlaceholder(config.getString("validator.label.fileInputPlaceholder", "Select file..."));
        domainConfig.getLabel().setTypeLabel(config.getString("validator.label.typeLabel", "Validate as"));
        domainConfig.getLabel().setUploadButton(config.getString("validator.label.uploadButton", "Validate"));
        domainConfig.getLabel().setResultSubSectionOverviewTitle(config.getString("validator.label.resultSubSectionOverviewTitle", "Overview"));
        domainConfig.getLabel().setResultDateLabel(config.getString("validator.label.resultDateLabel", "Date:"));
        domainConfig.getLabel().setResultFileNameLabel(config.getString("validator.label.resultFileNameLabel", "File name:"));
        domainConfig.getLabel().setResultResultLabel(config.getString("validator.label.resultResultLabel", "Result:"));
        domainConfig.getLabel().setResultErrorsLabel(config.getString("validator.label.resultErrorsLabel", "Errors:"));
        domainConfig.getLabel().setResultWarningsLabel(config.getString("validator.label.resultWarningsLabel", "Warnings:"));
        domainConfig.getLabel().setResultMessagesLabel(config.getString("validator.label.resultMessagesLabel", "Messages:"));
        domainConfig.getLabel().setViewAnnotatedInputButton(config.getString("validator.label.viewAnnotatedInputButton", "View annotated input"));
        domainConfig.getLabel().setDownloadXMLReportButton(config.getString("validator.label.downloadXMLReportButton", "Download XML report"));
        domainConfig.getLabel().setDownloadPDFReportButton(config.getString("validator.label.downloadPDFReportButton", "Download PDF report"));
        domainConfig.getLabel().setResultSubSectionDetailsTitle(config.getString("validator.label.resultSubSectionDetailsTitle", "Details"));
        domainConfig.getLabel().setResultTestLabel(config.getString("validator.label.resultTestLabel", "Test:"));
        domainConfig.getLabel().setPopupTitle(config.getString("validator.label.popupTitle", "XML content"));
        domainConfig.getLabel().setPopupCloseButton(config.getString("validator.label.popupCloseButton", "Close"));
        domainConfig.getLabel().setOptionContentFile(config.getString("validator.label.optionContentFile", "File"));
        domainConfig.getLabel().setOptionContentURI(config.getString("validator.label.optionContentURI", "URI"));
        domainConfig.getLabel().setOptionContentDirectInput(config.getString("validator.label.optionContentDirectInput", "Direct input"));
        domainConfig.getLabel().setResultValidationTypeLabel(config.getString("validator.label.resultValidationTypeLabel", "Validation type:"));
    }

    private Map<String, String> parseMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, String> map = new HashMap<>();
        for (String type: types) {
            String val = config.getString(key+"."+type, null);
            if (val != null) {
                map.put(type, config.getString(key+"."+type).trim());
            }
        }
        return map;
    }

    private class ExtensionFilter implements FilenameFilter {

        private String ext;

        public ExtensionFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
