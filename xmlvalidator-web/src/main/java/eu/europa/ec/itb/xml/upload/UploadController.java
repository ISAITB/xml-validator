package eu.europa.ec.itb.xml.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;

/**
 * Created by simatosc on 07/03/2016.
 */
@Controller
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

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
    @Autowired
	InputHelper inputHelper;

    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
    	setMinimalUIFlag(request, false);
    	DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("appConfig", appConfig);
        attributes.put("minimalUI", false);
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file, 
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string, 
    		@RequestParam(value = "validationType", defaultValue = "") String validationType,
    		@RequestParam(value = "contentType", defaultValue = "") String contentType, 
    		@RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaContentType,
    		@RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
    		@RequestParam(value = "uri-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
    		@RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchContentType,
    		@RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
    		@RequestParam(value = "uri-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request) {
		setMinimalUIFlag(request, false);
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("config", config);
        attributes.put("minimalUI", false);
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());
        if (StringUtils.isNotBlank(validationType)) {
            attributes.put("validationTypeLabel", config.getTypeLabel().get(validationType));
        }
        attributes.put("appConfig", appConfig);
		File tempFolderForRequest = fileManager.createTemporaryFolderPath();
        try {
			boolean proceedToValidate = true;
			if (StringUtils.isBlank(validationType)) {
				validationType = config.getType().get(0);
			}
			if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
				// A validation type is required.
				attributes.put("message", "Provided validation type is not valid");
				proceedToValidate = false;
			}
			if (proceedToValidate) {
				File inputFile = null;
				try {
					inputFile = saveInput(contentType, file.getInputStream(), uri, string, tempFolderForRequest);
					if (inputFile == null || !fileManager.checkFileType(inputFile)) {
						proceedToValidate = false;
						attributes.put("message", "Provided input is not an XML document");
					}
				} catch (IOException e) {
					logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
					attributes.put("message", "Error in upload [" + e.getMessage() + "]");
					proceedToValidate = false;
				}
				if (proceedToValidate) {
					List<FileInfo> externalSchIS = new ArrayList<>();
					List<FileInfo> externalSchemaIS = new ArrayList<>();
					try {
						externalSchemaIS = getExternalFiles(config, externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getSchemaInfo(validationType), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA, tempFolderForRequest);
						externalSchIS = getExternalFiles(config, externalSchContentType, externalSchFiles, externalSchUri, config.getSchematronInfo(validationType), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON, tempFolderForRequest);
					} catch (Exception e) {
						logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
						attributes.put("message", "Error in upload [" + e.getMessage() + "]");
						proceedToValidate = false;
					}
					if (proceedToValidate) {
						XMLValidator validator = beans.getBean(XMLValidator.class, inputFile, validationType, externalSchemaIS, externalSchIS, config);
						TAR report = validator.validateAll();
						attributes.put("report", report);
						attributes.put("date", report.getDate().toString());
						if (contentType.equals(contentType_file)) {
							attributes.put("fileName", file.getOriginalFilename());
						} else if(contentType.equals(contentType_uri)) {
							attributes.put("fileName", uri);
						} else {
							attributes.put("fileName", "-");
						}
						// Cache detailed report.
						try {
							String inputID = fileManager.writeXML(config.getDomainName(), report.getContext().getItem().get(0).getValue());
							attributes.put("inputID", inputID);
							fileManager.saveReport(report, inputID, config);
						} catch (IOException e) {
							logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
							attributes.put("message", "Error generating detailed report [" + e.getMessage() + "]");
						}
					}
				}
			}
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
        } finally {
        	// Cleanup temporary resources for request.
        	if (tempFolderForRequest.exists()) {
				FileUtils.deleteQuietly(tempFolderForRequest);
			}
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
        attributes.put("minimalUI", true);
        attributes.put("externalArtifactInfo", config.getExternalArtifactInfoMap());
        return new ModelAndView("uploadForm", attributes);
    }
    

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/uploadm")
    public ModelAndView handleUploadM(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file, 
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string, 
    		@RequestParam(value = "validationType", defaultValue = "") String validationType, 
    		@RequestParam(value = "contentType", defaultValue = "") String contentType, 
    		@RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchema,
    		@RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
    		@RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
    		@RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSch,
    		@RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
    		@RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request) {
    	
		setMinimalUIFlag(request, true);
		ModelAndView mv = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, externalSch, externalSchFiles, externalSchUri, redirectAttributes, request);
				
		Map<String, Object> attributes = mv.getModel();
        attributes.put("minimalUI", true);

        return new ModelAndView("uploadForm", attributes);	
	}

	private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
		if (request.getAttribute(IS_MINIMAL) == null) {
			request.setAttribute(IS_MINIMAL, isMinimal);
		}
	}

    private List<FileInfo> getExternalFiles(DomainConfig domainConfig, String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri,
											ValidationArtifactInfo artifactInfo, String validationType,
											String artifactType, File parentFolder) throws IOException {
    	List<FileInfo> artifacts = new ArrayList<>();
		if (externalContentType != null) {
			for (int i=0; i<externalContentType.length; i++) {
				File file = null;
				if (contentType_file.equals(externalContentType[i])) {
					if (!externalFiles[i].isEmpty()) {
						file = fileManager.getFileFromInputStream(parentFolder, externalFiles[i].getInputStream(), null, externalFiles[i].getOriginalFilename());
					}
				} else {
					if (StringUtils.isNotBlank(externalUri[i])) {
						file = fileManager.getFileFromURL(parentFolder, externalUri[i], null, null, artifactType);
					}
				}
				if (file != null) {
					File rootFile = this.fileManager.unzipFile(parentFolder, file);
					if (rootFile == null) {
						artifacts.add(new FileInfo(fileManager.preprocessFileIfNeeded(domainConfig, validationType, artifactType, file, true)));
					} else {
						// ZIP File
						boolean proceed = false;
						if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
							proceed = inputHelper.validateSchemaZip(rootFile);
						} else {
							proceed = true;
						}
						if (proceed) {
							artifacts.addAll(fileManager.getLocalValidationArtifacts(rootFile, DomainConfig.ARTIFACT_TYPE_SCHEMA));
						} else {
							logger.error("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");
							throw new IllegalStateException("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");
						}
					}
				}
            }
		}
    	if (validateExternalFiles(artifacts, artifactInfo, validationType) && (!DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType) || artifacts.size() <= 1)) {
        	return artifacts;
    	} else {
            logger.error("An error occurred during the validation of the external Schema.");
    		throw new IllegalStateException("An error occurred during the validation of the external Schema.");
    	}
    }

    private boolean validateExternalFiles(List<FileInfo> lis, ValidationArtifactInfo artifactInfo, String validationType) {
    	ExternalArtifactSupport externalArtifactSupport = artifactInfo.getExternalArtifactSupport();
		
    	boolean validated = false;
    	
    	switch(externalArtifactSupport) {
			case REQUIRED:
    			if(lis!=null && !lis.isEmpty()) {
    				validated = true;
    			}
    			break;
			case OPTIONAL:
    			validated = true;
    			break;
			case NONE:
    			if(lis==null || lis.isEmpty()) {
    				validated = true;
    			}
    			break;
    	}
    	
		return validated;
	}
    
	private File saveInput(String inputType, InputStream inputStream, String uri, String string, File parentFolder) throws IOException {
		File inputFile = null;
		switch (inputType) {
			case contentType_file:
		    	inputFile = fileManager.getFileFromInputStream(parentFolder, inputStream, null, null);
				break;
			case contentType_uri:
				inputFile = fileManager.getFileFromURL(parentFolder, uri);
				break;
			case contentType_string:
				inputFile = fileManager.getFileFromString(parentFolder, string);
				break;
		}
		return inputFile;
	}
    
}
