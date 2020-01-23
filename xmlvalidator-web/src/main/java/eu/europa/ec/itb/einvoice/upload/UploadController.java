package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.DomainConfigCache;
import eu.europa.ec.itb.einvoice.ValidatorChannel;
import eu.europa.ec.itb.einvoice.util.FileManager;
import eu.europa.ec.itb.einvoice.validation.FileInfo;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by simatosc on 07/03/2016.
 */
@Controller
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    public static final String IS_MINIMAL = "isMinimal";

    public static final String contentType_file     	= "fileType" ;
    public static final String contentType_uri     		= "uriType" ;
    public static final String contentType_string     	= "stringType" ;
    
    @Autowired
    FileManager fileManager;

    @Autowired
    BeanFactory beans;

    @Autowired
    DomainConfigCache domainConfigs;

    @Autowired
    ApplicationConfig appConfig;

    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
    	setMinimalUIFlag(request, false);
    	DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("minimalUI", false);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemaFile()));
        attributes.put("externalSchematron", includeExternalArtefacts(config.getExternalSchematronFile()));
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file, 
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string, 
    		@RequestParam(value = "validationType", defaultValue = "") String validationType,
    		@RequestParam(value = "contentType", defaultValue = "") String contentType, 
    		@RequestParam(value = "contentType-externalSchema", required = false) String[] externalSchemaContentType,
    		@RequestParam(value = "inputFile-externalSchema", required= false) MultipartFile[] externalSchemaFiles,
    		@RequestParam(value = "uriToValidate-externalSchema", required = false) String[] externalSchemaUri,
    		@RequestParam(value = "contentType-externalSch", required = false) String[] externalSchContentType,
    		@RequestParam(value = "inputFile-externalSch", required= false) MultipartFile[] externalSchFiles,
    		@RequestParam(value = "uriToValidate-externalSch", required = false) String[] externalSchUri,
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request) {
		setMinimalUIFlag(request, false);
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        InputStream stream = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("config", config);
        attributes.put("minimalUI", false);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemaFile()));
        attributes.put("externalSchematron", includeExternalArtefacts(config.getExternalSchematronFile()));
        
        if (StringUtils.isNotBlank(validationType)) {
            attributes.put("validationTypeLabel", config.getTypeLabel().get(validationType));
        }
        attributes.put("appConfig", appConfig);
        try {
        	InputStream fis = getInputStream(contentType, file.getInputStream(), uri, string);
            if (fileManager.checkFileType(fis)) {
                stream = getInputStream(contentType, file.getInputStream(), uri, string);
            } else {
                attributes.put("message", "Provided input is not an XML document");
            }
        } catch (IOException e) {
            logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
            attributes.put("message", "Error in upload [" + e.getMessage() + "]");
        }
        if (StringUtils.isBlank(validationType)) {
            validationType = config.getType().get(0);
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A invoice type is required.
            attributes.put("message", "Provided validation type is not valid");
        }
        try {
            if (stream != null) {
            	List<FileInfo> externalSchIS = new ArrayList<>();
            	List<FileInfo> externalSchemaIS = new ArrayList<>();
            	
            	try {
            		externalSchemaIS = getExternalFiles(externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getExternalSchemaFile(), validationType, true);
            		externalSchIS = getExternalFiles(externalSchContentType, externalSchFiles, externalSchUri, config.getExternalSchematronFile(), validationType, false);
            	} catch (Exception e) {
                    logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                    attributes.put("message", "Error in upload [" + e.getMessage() + "]");
                }
            	
                XMLValidator validator = beans.getBean(XMLValidator.class, stream, validationType, externalSchemaIS, externalSchIS, config);
                TAR report = validator.validateAll();
                attributes.put("report", report);
                attributes.put("date", report.getDate().toString());
                
                if(contentType.equals(contentType_file)) {
					attributes.put("fileName", file.getOriginalFilename());
				} else if(contentType.equals(contentType_uri)) {
					attributes.put("fileName", uri);
				} else {
					attributes.put("fileName", "-");
				}
                
                // Cache detailed report.
                try {
                    String xmlID = fileManager.writeXML(config.getDomainName(), report.getContext().getItem().get(0).getValue());
                    attributes.put("xmlID", xmlID);
                    fileManager.saveReport(report, xmlID);
                } catch (IOException e) {
                    logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                    attributes.put("message", "Error generating detailed report [" + e.getMessage() + "]");
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
        }
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/uploadm")
    public ModelAndView uploadm(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
		setMinimalUIFlag(request, true);

		DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        
		if(!config.isSupportMinimalUserInterface()) {
			logger.error("Minimal user interface is not supported in this domain [" + domain + "].");
			throw new NotFoundException();
		}
        
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("validationTypes", getValidationTypes(config));
        attributes.put("minimalUI", true);
        attributes.put("contentType", getContentType(config));
        attributes.put("externalSchema", includeExternalArtefacts(config.getExternalSchemaFile()));
        attributes.put("externalSchematron", includeExternalArtefacts(config.getExternalSchematronFile()));
        return new ModelAndView("uploadForm", attributes);
    }
    

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/uploadm")
    public ModelAndView handleUploadM(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file, 
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string, 
    		@RequestParam(value = "validationType", defaultValue = "") String validationType, 
    		@RequestParam(value = "contentType", defaultValue = "") String contentType, 
    		@RequestParam(value = "contentType-externalSchema", required = false) String[] externalSchema,
    		@RequestParam(value = "inputFile-externalSchema", required= false) MultipartFile[] externalSchemaFiles,
    		@RequestParam(value = "uriToValidate-externalSchema", required = false) String[] externalSchemaUri,
    		@RequestParam(value = "contentType-externalSch", required = false) String[] externalSch,
    		@RequestParam(value = "inputFile-externalSch", required= false) MultipartFile[] externalSchFiles,
    		@RequestParam(value = "uriToValidate-externalSch", required = false) String[] externalSchUri,
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request) {
    	
		setMinimalUIFlag(request, true);
		ModelAndView mv = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, externalSch, externalSchFiles, externalSchUri, redirectAttributes, request);
				
		Map<String, Object> attributes = mv.getModel();
        attributes.put("minimalUI", true);

        return new ModelAndView("uploadForm", attributes);	
	}
    
    private List<ValidationType> getValidationTypes(DomainConfig config) {
        List<ValidationType> types = new ArrayList<>();
        if (config.hasMultipleValidationTypes()) {
            for (String type: config.getType()) {
                types.add(new ValidationType(type, config.getTypeLabel().get(type)));
            }
        }
        return types;
    }
    
	private List<ValidationType> includeExternalArtefacts(Map<String, String> externalArtefact){
        List<ValidationType> types = new ArrayList<>();
    	
    	for(Map.Entry<String, String> entry : externalArtefact.entrySet()) {
    		types.add(new ValidationType(entry.getKey(), entry.getValue()));
    	}
    	
    	return types;
    }

	private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
		if (request.getAttribute(IS_MINIMAL) == null) {
			request.setAttribute(IS_MINIMAL, isMinimal);
		}
	}
    
    private List<ValidationType> getContentType(DomainConfig config){
        List<ValidationType> types = new ArrayList<>();

		types.add(new ValidationType(contentType_file, config.getLabel().getOptionContentFile()));
		types.add(new ValidationType(contentType_uri, config.getLabel().getOptionContentURI()));
		types.add(new ValidationType(contentType_string, config.getLabel().getOptionContentDirectInput()));
		
		return types;        
    }
    
    private List<FileInfo> getExternalFiles(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, 
    		Map<String, String> externalProperties, String validationType, boolean isSchema) throws Exception {
    	List<FileInfo> lis = new ArrayList<>();
    	
    	if(externalContentType != null) {
	    	for(int i=0; i<externalContentType.length; i++) {
				File inputFile = null;
				MultipartFile currentExtFile = null;
				String currentExtUri = "";
				
				if(externalFiles!=null && externalFiles.length>i) {
					currentExtFile = externalFiles[i];
				}
				if(externalUri!=null && externalUri.length>i) {
					currentExtUri = externalUri[i];
				}
				
				inputFile = getInputFile(externalContentType[i], currentExtFile, currentExtUri, isSchema);

				if(inputFile != null) {
					File rootFile = this.fileManager.unzipFile(inputFile);
					if(rootFile == null) {
						FileInfo fi = new FileInfo(inputFile, FilenameUtils.getExtension(inputFile.getName()));		    		
			    		lis.add(fi);
					}else {
						//ZIP File
						boolean isValid = false;
						if(isSchema) {
							isValid = validateSchemaZip(rootFile);
						}
						
						if(!isSchema || (isSchema && isValid)) {
							FileInfo fi = new FileInfo(rootFile, FilenameUtils.getExtension(rootFile.getName()));		    		
				    		lis.add(fi);
						}else {
				            logger.error("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");
				    		throw new Exception("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");							
						}
					}
				}
	    	}
    	}
    	
    	if (validateExternalFiles(lis, externalProperties, validationType) && ((isSchema && lis.size() <= 1) || !isSchema)) {
        	return lis;
    	}else { 
            logger.error("An error occurred during the validation of the external Schema.");
    		throw new Exception("An error occurred during the validation of the external Schema.");
    	}
    	
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
    
    private boolean validateExternalFiles(List<FileInfo> lis, Map<String, String> externalArtefacts, String validationType) {
    	String externalArtefactProperty = externalArtefacts.get(validationType);
		
    	boolean validated = false;
    	
    	switch(externalArtefactProperty) {
    		case DomainConfig.externalFile_req:
    			if(lis!=null && !lis.isEmpty()) {
    				validated = true;
    			}
    			break;
    		case DomainConfig.externalFile_opt:
    			validated = true;
    			break;
    		case DomainConfig.externalFile_none:
    			if(lis==null || lis.isEmpty()) {
    				validated = true;
    			}
    			break;
    	}
    	
		return validated;
	}
    
    private File getInputFile(String contentType, MultipartFile inputFile, String inputUri, boolean isSchema) throws IOException {
    	File f = null;
    	
    	switch(contentType) {
			case contentType_file:
				if(inputFile!=null && !inputFile.isEmpty()) {							
		        	f = this.fileManager.getInputStreamFile(inputFile.getInputStream(), inputFile.getOriginalFilename());				
				}
				break;
			case contentType_uri:					
				if(!inputUri.isEmpty()) {
					f = this.fileManager.getURLFile(inputUri, isSchema);
				}
				break;
		}
    	
    	return f;
    }
    
	private InputStream getInputStream(String contentType, InputStream inputStream, String uri, String string) {
		InputStream is = null;
		
		switch(contentType) {
			case contentType_file:
		    	is = inputStream;
				break;
			
			case contentType_uri:
				is = this.fileManager.getURIInputStream(uri);
				break;
				
			case contentType_string:
				is = new ByteArrayInputStream(string.getBytes());
				break;
		}

		return is;
	}
    
}
