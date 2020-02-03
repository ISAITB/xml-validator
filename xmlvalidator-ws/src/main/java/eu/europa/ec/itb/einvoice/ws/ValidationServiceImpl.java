package eu.europa.ec.itb.einvoice.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.util.FileManager;
import eu.europa.ec.itb.einvoice.validation.FileInfo;
import eu.europa.ec.itb.einvoice.validation.ValidationConstants;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by simatosc on 25/02/2016.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements com.gitb.vs.ValidationService {

    private final DomainConfig domainConfig;

    @Autowired
    ApplicationContext ctx;
    @Autowired
    ApplicationConfig config;
    @Autowired
	FileManager fileManager;
    
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
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_TYPE, "string", usage, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_TYPE)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_XML, "object", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_XML)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));

        if (supportsExternalArtifacts(domainConfig.getExternalSchemaFile())) {
            response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMA, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMA)));
        }
        if (supportsExternalArtifacts(domainConfig.getExternalSchematronFile())) {
            response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON)));
        }
        return response;
    }

    private boolean supportsExternalArtifacts(Map<String, DomainConfig.ExternalValidationArtifactInfo> artifactInfoMap) {
        for (DomainConfig.ExternalValidationArtifactInfo artifactInfo: artifactInfoMap.values()) {
            if (DomainConfig.externalFile_req.equals(artifactInfo.getSupportForExternalArtifacts())
                    || DomainConfig.externalFile_opt.equals(artifactInfo.getSupportForExternalArtifacts())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind of parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    private TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        MDC.put("domain", domainConfig.getDomainName());
        List<AnyContent> fileInputs = getXMLInput(validateRequest);
        String validationType = null;
        if (fileInputs.isEmpty()) {
            throw new IllegalArgumentException("You must provide the file to validate");
        }
        if (fileInputs.size() > 1) {
            throw new IllegalArgumentException("A single input file is expected");
        }
        if (domainConfig.hasMultipleValidationTypes()) {
            List<AnyContent> validationTypeInputs = getTypeInput(validateRequest);
            if (validationTypeInputs.isEmpty()) {
                throw new IllegalArgumentException("You must provide the type of validation to perform");
            }
            if (validationTypeInputs.size() > 1) {
                throw new IllegalArgumentException("A single validation type is expected");
            }
            validationType = validationTypeInputs.get(0).getValue();
            if (!domainConfig.getType().contains(validationType)) {
                throw new IllegalArgumentException("Invalid validation type provided ["+validationType+"]");
            }
        } else {
            validationType = domainConfig.getType().get(0);
        }
        String contentToValidate;
        try {
            contentToValidate = extractContent(fileInputs.get(0), getEmbeddingMethodInput(validateRequest.getInput())).trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read provided input", e);
        }
        // Get and validate any externally provided validation artifacts
        List<FileInfo> externalSchema = getExternalFiles(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA, validationType, "xsd");
        List<FileInfo> externalSch = getExternalFiles(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, validationType, "sch");
        // Proceed with the validation.
        XMLValidator validator = ctx.getBean(XMLValidator.class, new ByteArrayInputStream(contentToValidate.getBytes(StandardCharsets.UTF_8)), validationType, externalSchema, externalSch, domainConfig);
        TAR report = validator.validateAll();
        ValidationResponse result = new ValidationResponse();
        result.setReport(report);
        return result;
    }

    private List<AnyContent> getXMLInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_XML);
    }

    private List<AnyContent> getTypeInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_TYPE);
    }

    private ValueEmbeddingEnumeration getEmbeddingMethodInput(List<AnyContent> inputs) {
        ValueEmbeddingEnumeration result = null;
        if (inputs != null) {
            List<AnyContent> foundInputs = getInputFor(inputs, ValidationConstants.INPUT_EMBEDDING_METHOD);
            if (!foundInputs.isEmpty()) {
                result = ValueEmbeddingEnumeration.fromValue(foundInputs.get(0).getValue());
            }
        }
        return result;
    }

    private List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (validateRequest != null) {
            if (validateRequest.getInput() != null) {
                for (AnyContent anInput: validateRequest.getInput()) {
                    if (name.equals(anInput.getName())) {
                        inputs.add(anInput);
                    }
                }
            }
        }
        return inputs;
    }

    private List<AnyContent> getInputFor(List<AnyContent> inputsToConsider, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (inputsToConsider != null) {
            for (AnyContent anInput: inputsToConsider) {
                if (name.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

    private List<FileInfo> getExternalFiles(ValidateRequest validateRequest, String name, String validationType, String defaultContentType) {
    	List<FileInfo> filesContent = new ArrayList<>();
    	List<AnyContent> listInput = getInputFor(validateRequest, name);
        List<AnyContent> listInputContent;
        if (!listInput.isEmpty()) {
            listInputContent = listInput.get(0).getItem();
        } else {
            listInputContent = new ArrayList<>(0);
        }
        String externalArtifactSupport = (ValidationConstants.INPUT_EXTERNAL_SCHEMA.equals(name)?domainConfig.getExternalSchemaFile():domainConfig.getExternalSchematronFile()).get(validationType).getSupportForExternalArtifacts();
        if (DomainConfig.externalFile_none.equals(externalArtifactSupport) && (!listInput.isEmpty() || !listInputContent.isEmpty())) {
            throw new IllegalArgumentException("Validation artifact(s) were provided for ["+name+"] when none are expected.");
        } else if (DomainConfig.externalFile_req.equals(externalArtifactSupport) && listInputContent.isEmpty()) {
            throw new IllegalArgumentException("No validation artifact(s) were provided for ["+name+"].");
        }
        for (AnyContent inputContent: listInputContent) {
            /*
              This is a map with two items:
              - "content": The content to consider.
              - "type": For schemas this is "zip" or "xsd" whereas for schematron this is "sch" or "xsl".
             */
            List<AnyContent> contentInput =  getInputFor(inputContent.getItem(), "content");
            ValueEmbeddingEnumeration method = getEmbeddingMethodInput(inputContent.getItem());
            if (contentInput.size() != 1) {
                throw new IllegalArgumentException("A single \"content\" input is expected per provided validation artifact");
            }
            String type = null;
            List<AnyContent> typeInput =  getInputFor(inputContent.getItem(), "type");
            if (!typeInput.isEmpty()) {
                try {
                    type = extractContent(typeInput.get(0), null);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to extract the \"type\" input for a provided validation artifact", e);
                }
            }
            if (type == null) {
                type = defaultContentType;
            }
            if (ValidationConstants.INPUT_EXTERNAL_SCHEMA.equals(name) && !"xsd".equals(type) && !"zip".equals(type)) {
                throw new IllegalArgumentException("Invalid type value for provided XSD ["+type+"]");
            } else if (ValidationConstants.INPUT_EXTERNAL_SCHEMATRON.equals(name) && !"sch".equals(type) && !"xsl".equals(type)) {
                throw new IllegalArgumentException("Invalid type value for provided schematron ["+type+"]");
            }
            try {
                FileInfo fileContent = getExternalFileInfo(contentInput.get(0), type, name, method);
                if (fileContent.getFile() != null) {
                    filesContent.add(fileContent);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to extract the content for a provided validation artifact", e);
            }
        }
    	return filesContent;
    }

    private FileInfo getExternalFileInfo(AnyContent content, String type, String name, ValueEmbeddingEnumeration method) throws IOException, URISyntaxException {
    	FileInfo fileContent = new FileInfo();
        if (ValidationConstants.INPUT_EXTERNAL_SCHEMATRON.equals(name)) {
            String stringContent = extractContent(content, method);
            String mimeType = fileManager.checkContentType(stringContent);
            if (!config.getAcceptedSchematronMimeType().contains(mimeType)) {
                throw new IllegalArgumentException("Unsupported mime type ["+mimeType+"] for provided schematron");
            }
            fileContent.setFile(fileManager.getStringFile(stringContent, type));
        } else {
            if ("xsd".equals(type)) {
                String stringContent = extractContent(content, method);
                String mimeType = fileManager.checkContentType(stringContent);
                if (!config.getAcceptedSchemaMimeType().contains(mimeType)) {
                    throw new IllegalArgumentException("Unsupported mime type ["+mimeType+"] for provided schema");
                }
                fileContent.setFile(fileManager.getStringFile(stringContent, type));
            } else {
                // zip - can only be provided as URI or BASE64
                ValueEmbeddingEnumeration contentType = (method!=null)?method:content.getEmbeddingMethod();
                if (ValueEmbeddingEnumeration.STRING.equals(contentType)) {
                    throw new IllegalArgumentException("A zip archive containing the XSD cannot be provided with an embedding method of STRING");
                } else {
                    File zipFile;
                    if (ValueEmbeddingEnumeration.BASE_64.equals(contentType)) {
                        byte[] contentBytes = Base64.decodeBase64(content.getValue());
                        String mimeType = fileManager.checkContentType(contentBytes);
                        if (config.getAcceptedZipMimeType().contains(mimeType)) {
                            throw new IllegalArgumentException("Unexpected mime type ["+mimeType+"] for XSD zip archive");
                        }
                        zipFile = fileManager.unzipFile(contentBytes);
                    } else {
                        String mimeType = fileManager.checkContentTypeUrl(content.getValue());
                        if (config.getAcceptedZipMimeType().contains(mimeType)) {
                            throw new IllegalArgumentException("Unexpected mime type ["+mimeType+"] for XSD zip archive");
                        }
                        zipFile = fileManager.unzipFile(fileManager.getURLFile(content.getValue(), false));
                    }
                    if (validateSchemaZip(zipFile)) {
                        fileContent.setFile(zipFile);
                    } else {
                        throw new IllegalArgumentException("When XSD configuration is provided as a ZIP archive it needs to include a single XSD at its root (and any other folders with imported XSDs)");
                    }
                }
            }
        }
    	return fileContent;
    }
    
    private boolean validateSchemaZip(File rootFolder) {
    	int iRootFiles = 0;
    	
    	//1 file as root, other files in a folder.
		if (rootFolder.isFile()) {
		     iRootFiles++;
		} else {
		     // List all files.
		     File[] files = rootFolder.listFiles();
		     if (files != null) {
		         for (File aSchemaFile: files) {
		             if (aSchemaFile.isFile()) {
		                 iRootFiles++;
		             }
		         }
		     }
		}
    	
    	return iRootFiles == 1;
    }

    /**
     * Extract the String represented by the provided content.
     *
     * @param content The content to process.
     * @param method The embeding method to consider (can be null).
     * @return The content's String representation.
     */
    private String extractContent(AnyContent content, ValueEmbeddingEnumeration method) throws IOException {
        String stringContent = null;
        if (content != null && content.getValue() != null) {
            ValueEmbeddingEnumeration methodToCheck = (method == null)?content.getEmbeddingMethod():method;
            switch (methodToCheck) {
                case STRING:
                    // Use string as-is.
                    stringContent = content.getValue();
                    break;
                case URI:
                    // Read the string from the provided URI.
                    URI uri = URI.create(content.getValue());
                    Proxy proxy = null;
                    List<Proxy> proxies = ProxySelector.getDefault().select(uri);
                    if (proxies != null && !proxies.isEmpty()) {
                        proxy = proxies.get(0);
                    }
                    BufferedReader in = null;
                    try {
                        URLConnection connection;
                        if (proxy == null) {
                            connection = uri.toURL().openConnection();
                        } else {
                            connection = uri.toURL().openConnection(proxy);
                        }
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder builder = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            builder.append(inputLine);
                        }
                        stringContent = builder.toString();
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Unable to read provided URI", e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                // Ignore.
                            }
                        }
                    }
                    break;
                default: // BASE_64
                    // Construct the string from its BASE64 encoded bytes.
                    char[] buffer = new char[1024];
                    int numCharsRead;
                    StringBuilder sb = new StringBuilder();
                    try (BomStrippingReader reader = new BomStrippingReader(new ByteArrayInputStream(Base64.decodeBase64(content.getValue())))) {
                        while ((numCharsRead = reader.read(buffer, 0, buffer.length)) != -1) {
                            sb.append(buffer, 0, numCharsRead);
                        }
                    }
                    stringContent = sb.toString();
                    break;
            }
        }
        return stringContent;
    }


}
