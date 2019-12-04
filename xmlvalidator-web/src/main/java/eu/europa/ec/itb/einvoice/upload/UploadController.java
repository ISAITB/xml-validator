package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.DomainConfigCache;
import eu.europa.ec.itb.einvoice.ValidatorChannel;
import eu.europa.ec.itb.einvoice.util.FileManager;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
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
        	InputStream fis = getInputFile(contentType, file.getInputStream(), uri, string);
            if (fileManager.checkFileType(fis)) {
                stream = getInputFile(contentType, file.getInputStream(), uri, string);
            } else {
                attributes.put("message", "Provided input is not an XML document");
            }
        } catch (IOException e) {
            logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
            attributes.put("message", "Error in upload [" + e.getMessage() + "]");
        }
        if (StringUtils.isBlank(validationType)) {
            validationType = null;
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A invoice type is required.
            attributes.put("message", "Provided validation type is not valid");
        }
        try {
            if (stream != null) {
                XMLValidator validator = beans.getBean(XMLValidator.class, stream, validationType, config);
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
    		RedirectAttributes redirectAttributes,
    		HttpServletRequest request) {
    	
		setMinimalUIFlag(request, true);
		ModelAndView mv = handleUpload(domain, file, uri, string, validationType, contentType, redirectAttributes, request);
				
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
    
	private List<ValidationType> includeExternalArtefacts(Map<String, Boolean> externalArtefact){
        List<ValidationType> types = new ArrayList<>();
    	
    	for(Map.Entry<String, Boolean> entry : externalArtefact.entrySet()) {
    		types.add(new ValidationType(entry.getKey(), entry.getValue().toString()));
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
    
	private InputStream getInputFile(String contentType, InputStream inputStream, String uri, String string) {
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
