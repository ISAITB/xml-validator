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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import java.io.*;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by simatosc on 25/02/2016.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements com.gitb.vs.ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
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
        if (domainConfig.hasMultipleValidationTypes()) {
            TypedParameter xmlInput =  new TypedParameter();
            xmlInput.setName(ValidationConstants.INPUT_TYPE);
            xmlInput.setType("string");
            xmlInput.setUse(UsageEnumeration.R);
            xmlInput.setKind(ConfigurationType.SIMPLE);
            xmlInput.setDesc(domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_TYPE));
            response.getModule().getInputs().getParam().add(xmlInput);
        }
        TypedParameter xmlInput =  new TypedParameter();
        xmlInput.setName(ValidationConstants.INPUT_XML);
        xmlInput.setType("object");
        xmlInput.setUse(UsageEnumeration.R);
        xmlInput.setKind(ConfigurationType.SIMPLE);
        xmlInput.setDesc(domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_XML));
        response.getModule().getInputs().getParam().add(xmlInput);
        return response;
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
        }
        String invoiceToValidate;
        List<FileInfo> externalSchema = new ArrayList<>();
        List<FileInfo> externalSch = new ArrayList<>();
        try {
            invoiceToValidate = extractContent(fileInputs.get(0)).trim();
            externalSchema = validateExternalFiles(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMA, validationType);
            externalSch = validateExternalFiles(validateRequest, ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, validationType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read provided input", e);
        } catch(Exception e) {
            throw new IllegalArgumentException("An error occurred during the validation of the external Schema.");
        }
        XMLValidator validator;
        validator = ctx.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes(StandardCharsets.UTF_8)), validationType, externalSchema, externalSch, domainConfig);
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
    
    private List<FileInfo> validateExternalFiles(ValidateRequest validateRequest, String name, String validationType) throws Exception{
    	List<FileInfo> filesContent = new ArrayList<>();
    	List<AnyContent> listInput = getInputFor(validateRequest, name);

    	boolean isValid = validExternalFiles(validationType, name, listInput);
    	if(!isValid) {
            logger.error("An error occurred during the validation of the external Schema.");
    		throw new Exception("An error occurred during the validation of the external Schema.");
    	}
    		
    	try {
		    	if(!listInput.isEmpty()) {
			    	AnyContent listRuleSets = listInput.get(0);
		
					if (listRuleSets.getItem() != null && !listRuleSets.getItem().isEmpty()) {
		
						//Validate variables and ruleSets
						for (AnyContent ruleSet : listRuleSets.getItem()) {
							FileInfo fileContent = getFileInfo(ruleSet, name);
							
							if (fileContent.getFile()!=null) {
								filesContent.add(fileContent);
							}
						}
					}
		    	}else {
		    		return Collections.emptyList();
		    	}
    	}catch(Exception e) {
            logger.error("Error while reading uploaded external file [" + e.getMessage() + "]", e);
            
    		return Collections.emptyList();
    	}
    	
    	return filesContent;
    }
    
    private boolean validExternalFiles(String validationType, String name, List<AnyContent> listRuleSet) {
    	boolean isValid = false;
    	String externalSchema = domainConfig.getExternalSchemaFile().get(validationType);
    	
    	if(ValidationConstants.INPUT_EXTERNAL_SCHEMA.contentEquals(name)) {
    		isValid = validExternalSchemaFiles(externalSchema, listRuleSet);
    	}
    	if(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON.contentEquals(name)) {
    		isValid = validExternalSchematronFiles(externalSchema, listRuleSet);
    	}
    	
    	return isValid;
    }
    
    private boolean validExternalSchematronFiles(String externalRequirement, List<AnyContent> ruleSet) {
    	boolean isValid = false;
    	boolean existRuleSet = !ruleSet.isEmpty();
    	boolean existValues = false;
    	
    	if(existRuleSet) {
    		existValues = (ruleSet.get(0).getItem()!=null && !ruleSet.get(0).getItem().isEmpty());
    	}
    	
    	if(DomainConfig.externalFile_none.equals(externalRequirement) && !existRuleSet) {
    		isValid = true;
    	}
    	
    	if(DomainConfig.externalFile_req.equals(externalRequirement) && existValues) {
    		isValid = true;
    	}
    	
    	if(DomainConfig.externalFile_opt.equals(externalRequirement)) {
    		isValid = true;
    	}
    	
    	return isValid;
    }
    
    private boolean validExternalSchemaFiles(String externalRequirement, List<AnyContent> ruleSet) {
    	boolean isValid = false;
    	boolean existRuleSet = !ruleSet.isEmpty();
    	int values = 0;

    	if(existRuleSet) {
    		if(ruleSet.get(0).getItem()!=null && !ruleSet.get(0).getItem().isEmpty()) {
    			values = ruleSet.get(0).getItem().size();
    		}
    	}
    	
    	if(DomainConfig.externalFile_none.equals(externalRequirement) && !existRuleSet && values==0) {
    		isValid = true;
    	}
    	
    	if(DomainConfig.externalFile_req.equals(externalRequirement) && existRuleSet && values==1) {
    		isValid = true;
    	}
    	
    	if(DomainConfig.externalFile_opt.equals(externalRequirement) && values<=1) {
    		isValid = true;
    	}
    	
    	return isValid;
    }
    
    private FileInfo getFileInfo(AnyContent content, String name) throws Exception {
    	FileInfo fileContent = new FileInfo();
    	ValueEmbeddingEnumeration contentType = content.getEmbeddingMethod();
    	
    	if(contentType.equals(ValueEmbeddingEnumeration.URI)) {
    		String mimeType = fileManager.checkContentTypeUrl(content.getValue());
    		
    		if(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON.equals(name)) {
    			if(config.getAcceptedSchematronSyntax().contains(mimeType)) {
	    			File f = fileManager.getURLFile(content.getValue(), false);
	    			fileContent.setFile(f);    				
    			}
    		}else {
	    		if(config.getAcceptedZipSyntax().contains(mimeType)) {
	    			//ZIP file
	    			File f = fileManager.getURLFile(content.getValue(), false);
	    			File zipFile = fileManager.unzipFile(f);
	    			//Validate ZIP file    			
					boolean isValid = validateSchemaZip(zipFile);
					
					if(isValid) {
		    			fileContent.setFile(zipFile);
					}else {
			            logger.error("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");
			    		throw new Exception("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");							
					}
	    		}
	    		if(config.getAcceptedSchemaSyntax().contains(mimeType)) {
	    			//XSD
	    			File f = fileManager.getURLFile(content.getValue(), true);
	    			fileContent.setFile(f);
	    		}
    		}
    	}else {
        	String sContent = extractContent(content);
    		String mimeType = fileManager.checkContentType(sContent);
    		
    		if(contentType.equals(ValueEmbeddingEnumeration.BASE_64) || contentType.equals(ValueEmbeddingEnumeration.STRING)) {
    			if(ValidationConstants.INPUT_EXTERNAL_SCHEMA.equals(name) && config.getAcceptedSchemaSyntax().contains(mimeType)) {
    				File f = fileManager.getStringFile(sContent, "");
    		    	fileContent.setFile(f);
    			}
    			if(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON.equals(name) && config.getAcceptedSchematronSyntax().contains(mimeType)) {
    		    	File f = fileManager.getStringFile(sContent, "xsl");
    		    	fileContent.setFile(f);
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
    	
    	return (iRootFiles>1)? false : true;
    }

    /**
     * Extract the String represented by the provided content.
     *
     * @param content The content to process.
     * @return The content's String representation.
     */
    private String extractContent(AnyContent content) throws IOException {
        String stringContent = null;
        if (content != null && content.getValue() != null) {
            switch (content.getEmbeddingMethod()) {
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
