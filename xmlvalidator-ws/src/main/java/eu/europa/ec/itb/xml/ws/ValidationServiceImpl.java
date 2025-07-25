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

package eu.europa.ec.itb.xml.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.WebServiceContextProvider;
import eu.europa.ec.itb.xml.ContextFileData;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.ValidationSpecs;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.ValidationConstants;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import jakarta.annotation.Resource;
import jakarta.jws.WebParam;
import jakarta.xml.ws.WebServiceContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by simatosc on 25/02/2016.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements com.gitb.vs.ValidationService, WebServiceContextProvider {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;
    private final DomainConfig requestedDomainConfig;

    @Autowired
    InputHelper inputHelper;
    @Autowired
    ApplicationContext ctx;
    @Autowired
	FileManager fileManager;
    @Resource
    WebServiceContext wsContext;

    /**
     * Constructor.
     *
     * @param domainConfig The domain configuration (each domain has its own instance).
     * @param requestedDomainConfig The resolved domain configuration (in case of aliases).
     */
    public ValidationServiceImpl(DomainConfig domainConfig, DomainConfig requestedDomainConfig) {
        this.domainConfig = domainConfig;
        this.requestedDomainConfig = requestedDomainConfig;
    }

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        MDC.put("domain", domainConfig.getDomainName());
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        domainConfig.applyWebServiceMetadata(response.getModule());
        response.getModule().setInputs(new TypedParameters());
        UsageEnumeration usage = UsageEnumeration.O;
        if (domainConfig.hasMultipleValidationTypes() && domainConfig.getDefaultType() == null) {
            usage = UsageEnumeration.R;
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_TYPE, "string", usage, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_TYPE)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_XML, "object", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_XML)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCATION_AS_PATH, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCATION_AS_PATH)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_SHOW_LOCATION_PATHS, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_SHOW_LOCATION_PATHS)));
        if (inputHelper.supportsExternalArtifacts(domainConfig.getArtifactInfo(), DomainConfig.ARTIFACT_TYPE_SCHEMA)) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA)));
        }
        if (inputHelper.supportsExternalArtifacts(domainConfig.getArtifactInfo(), DomainConfig.ARTIFACT_TYPE_SCHEMATRON)) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON)));
        }
        if (domainConfig.hasContextFiles()) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTEXT_FILES, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTEXT_FILES)));
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCALE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCALE)));
        return response;
    }


    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        MDC.put("domain", domainConfig.getDomainName());
        File tempFolderPath = fileManager.createTemporaryFolderPath();
        var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(getInputAsString(validateRequest, ValidationConstants.INPUT_LOCALE, null)), domainConfig));
        try {
            ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
            boolean locationAsPath = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_LOCATION_AS_PATH, false);
            boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, true);
            boolean showLocationPaths = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_SHOW_LOCATION_PATHS, domainConfig.isIncludeLocationPath());
            File contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_XML, contentEmbeddingMethod, null, tempFolderPath, domainConfig.getHttpVersion()).getFile();
            String validationType = inputHelper.validateValidationType(requestedDomainConfig.getDomainName(), domainConfig, validateRequest, ValidationConstants.INPUT_TYPE);
            List<FileInfo> externalSchemas = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA, ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA, tempFolderPath);
            List<FileInfo> externalSchematron = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON, tempFolderPath);
            List<ContextFileData> contextFiles = getContextFiles(domainConfig, validateRequest, validationType, tempFolderPath);
            // Proceed with the validation.
            ValidationSpecs specs = ValidationSpecs.builder(contentToValidate, localiser, domainConfig, ctx)
                    .withValidationType(validationType)
                    .withExternalSchemas(externalSchemas)
                    .withExternalSchematrons(externalSchematron)
                    .locationAsPath(locationAsPath)
                    .addInputToReport(addInputToReport)
                    .showLocationPaths(showLocationPaths)
                    .withContextFiles(contextFiles)
                    .withTempFolder(tempFolderPath.toPath())
                    .build();
            XMLValidator validator = ctx.getBean(XMLValidator.class, specs);
            TAR report = validator.validateAll();
            ValidationResponse result = new ValidationResponse();
            result.setReport(report);
            return result;
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            throw new ValidatorException(e.getMessageForDisplay(localiser), true);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            var message = localiser.localise(ValidatorException.MESSAGE_DEFAULT);
            throw new ValidatorException(message, e, true, (Object[]) null);
        } finally {
            // Cleanup.
            if (tempFolderPath.exists()) {
                FileUtils.deleteQuietly(tempFolderPath);
            }
        }

    }

    /**
     * Get the provided (optional) input as a boolean value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private boolean getInputAsBoolean(ValidateRequest validateRequest, String inputName, boolean defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return Boolean.parseBoolean(input.get(0).getValue());
        }
        return defaultIfMissing;
    }

    /**
     * Get the provided (optional) input as a string value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private String getInputAsString(ValidateRequest validateRequest, String inputName, String defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return input.get(0).getValue();
        }
        return defaultIfMissing;
    }

    /**
     * Get the submitted context files.
     *
     * @param config The domain configuration.
     * @param validateRequest The received request.
     * @param validationType The validation type.
     * @param parentFolder The temporary folder to consider.
     * @return The list of context files.
     * @throws IOException If an IO error occurs.
     */
    private List<ContextFileData> getContextFiles(DomainConfig config, ValidateRequest validateRequest, String validationType, File parentFolder) throws IOException {
        var contextFileConfigs = config.getContextFiles(validationType);
        if (contextFileConfigs.isEmpty()) {
            return Collections.emptyList();
        } else {
            int expectedContextFiles = contextFileConfigs.size();
            var receivedContextFiles = collectContextFiles(validateRequest);
            if (expectedContextFiles != receivedContextFiles.size()) {
                var exception = new ValidatorException("validator.label.exception.wrongContextFileCount");
                logger.error(exception.getMessageForLog());
                throw exception;
            } else {
                int index = 0;
                List<ContextFileData> contextFiles = new ArrayList<>();
                for (var contextFileConfig: contextFileConfigs) {
                    var targetFile = Path.of(parentFolder.getPath(), "contextFiles").resolve(contextFileConfig.path()).toFile();
                    var receivedContextFile = receivedContextFiles.get(index);
                    switch (receivedContextFile.getEmbeddingMethod()) {
                        case BASE_64 -> fileManager.getFileFromBase64(targetFile.getParentFile(), receivedContextFile.getContent(), FileManager.EXTERNAL_FILE, targetFile.getName());
                        case URI -> fileManager.getFileFromURL(targetFile.getParentFile(), receivedContextFile.getContent(), "", targetFile.getName(), domainConfig.getHttpVersion());
                        default -> fileManager.getFileFromString(targetFile.getParentFile(), receivedContextFile.getContent(), FileManager.EXTERNAL_FILE, targetFile.getName());
                    }
                    contextFiles.add(new ContextFileData(targetFile.toPath(), contextFileConfig));
                    index += 1;
                }
                return contextFiles;
            }
        }
    }

    /**
     * Collect the context files from the inputs.
     *
     * @param validateRequest The input request.
     * @return The context files.
     */
    private List<FileContent> collectContextFiles(ValidateRequest validateRequest) {
        return inputHelper.toExternalArtifactContents(validateRequest, ValidationConstants.INPUT_CONTEXT_FILES, ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT, ValidationConstants.INPUT_EMBEDDING_METHOD);
    }

    @Override
    public WebServiceContext getWebServiceContext() {
        return this.wsContext;
    }

}
