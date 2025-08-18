/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml.config;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.ValidationSpecs;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;

/**
 * Configuration class to trigger the preloading of XSD imports for the domains where this is enabled.
 */
@Configuration
public class ResourcePreloader {

    private static final Logger LOG = LoggerFactory.getLogger(ResourcePreloader.class);

    @Autowired
    private DomainConfigCache domainConfigs = null;
    @Autowired
    private ApplicationContext ctx = null;
    @Autowired
    FileManager fileManager;
    @Autowired
    private PluginManager pluginManager = null;

    @PostConstruct
    public void initialise() {
        // Initialise plugins.
        if (pluginManager.hasPlugins()) {
            LOG.info("Initialised plugins");
        }
        // Preload XSD imports.
        domainConfigs.getAllDomainConfigurations().stream().filter(DomainConfig::isPreloadingRemoteSchemaImportsForAnyType).forEach(domainConfig -> {
            LOG.info("Preloading remote schema imports for domain [{}]", domainConfig.getDomainName());
            var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(null, domainConfig));
            // Iterate over validation types.
            domainConfig.getType().stream().filter(domainConfig::preloadRemoteSchemaImports).forEach(validationType -> {
                // Trigger the preloading and caching of import references by making a XSD-only validation of dummy content.
                LOG.info("Preloading remote schema imports for validation type [{}]", validationType);
                File tempFolderForRequest = fileManager.createTemporaryFolderPath();
                try {
                    // Prepare a dummy empty XML file to trigger the validation.
                    File inputFile = fileManager.getFileFromString(tempFolderForRequest, "<empty/>");
                    ValidationSpecs specs = ValidationSpecs.builder(inputFile, localiser, domainConfig, ctx)
                            .addInputToReport(false)
                            .locationAsPath(true)
                            .withTempFolder(tempFolderForRequest.toPath())
                            .withValidationType(validationType)
                            .skipSchematronValidation()
                            .skipPluginValidation()
                            .skipProgressLogging()
                            .build();
                    XMLValidator validator = ctx.getBean(XMLValidator.class, specs);
                    validator.validateAll();
                } catch (Exception e) {
                    LOG.warn("Failed to preload remote schema imports for domain [{}]", domainConfig.getDomainName(), e);
                } finally {
                    // Cleanup temporary resources for request.
                    if (Files.exists(tempFolderForRequest.toPath())) {
                        FileUtils.deleteQuietly(tempFolderForRequest);
                    }
                }
            });
        });
    }

}
