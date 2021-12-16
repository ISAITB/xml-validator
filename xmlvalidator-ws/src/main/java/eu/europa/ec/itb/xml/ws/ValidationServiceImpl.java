package eu.europa.ec.itb.xml.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.ValidationConstants;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceContext;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Created by simatosc on 25/02/2016.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements com.gitb.vs.ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
    InputHelper inputHelper;
    @Autowired
    ApplicationContext ctx;
    @Autowired
	FileManager fileManager;
    @Resource
    WebServiceContext wsContext;

    public ValidationServiceImpl(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        MDC.put("domain", domainConfig.getDomainName());
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(domainConfig.getWebServiceId());
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(domainConfig.getWebServiceId());
        response.getModule().getMetadata().setVersion("1.0.0");
        response.getModule().setInputs(new TypedParameters());
        UsageEnumeration usage = UsageEnumeration.O;
        if (domainConfig.hasMultipleValidationTypes()) {
            usage = UsageEnumeration.R;
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_TYPE, "string", usage, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_TYPE)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_XML, "object", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_XML)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCATION_AS_PATH, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCATION_AS_PATH)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));

        if (inputHelper.supportsExternalArtifacts(domainConfig.getArtifactInfo(), DomainConfig.ARTIFACT_TYPE_SCHEMA)) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA)));
        }
        if (inputHelper.supportsExternalArtifacts(domainConfig.getArtifactInfo(), DomainConfig.ARTIFACT_TYPE_SCHEMATRON)) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON)));
        }
        return response;
    }


    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        MDC.put("domain", domainConfig.getDomainName());
        File tempFolderPath = fileManager.createTemporaryFolderPath();
        try {
            ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
            boolean locationAsPath = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_LOCATION_AS_PATH, false);
            boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, true);
            File contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_XML, contentEmbeddingMethod, tempFolderPath);
            String validationType = inputHelper.validateValidationType(domainConfig, validateRequest, ValidationConstants.INPUT_TYPE);
            List<FileInfo> externalSchemas = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA, ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA, tempFolderPath);
            List<FileInfo> externalSchematron = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON, tempFolderPath);
            // Proceed with the validation.
            XMLValidator validator = ctx.getBean(XMLValidator.class, contentToValidate, validationType, externalSchemas, externalSchematron, domainConfig, locationAsPath, addInputToReport, new LocalisationHelper(domainConfig, Locale.ENGLISH));
            TAR report = validator.validateAll();
            ValidationResponse result = new ValidationResponse();
            result.setReport(report);
            return result;
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            throw new ValidatorException(e.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH)), true);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            var message = new LocalisationHelper(Locale.ENGLISH).localise(ValidatorException.MESSAGE_DEFAULT);
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

    public WebServiceContext getWebServiceContext() {
        return this.wsContext;
    }

}
