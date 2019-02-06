package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.DomainConfigCache;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by simatosc on 07/03/2016.
 */
@Controller
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    FileManager fileManager;

    @Autowired
    BeanFactory beans;

    @Autowired
    DomainConfigCache domainConfigs;

    @RequestMapping(method = RequestMethod.GET, value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model) {
        DomainConfig config = domainConfigs.getConfigForDomain(domain);
        if (config == null) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        Map<String, Object> attributes = new HashMap<String, Object>();
//        attributes.put("title", config.getUploadTitle());
        attributes.put("config", config);
        attributes.put("validationTypes", getValidationTypes(config));
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain, @RequestParam("file") MultipartFile file, @RequestParam(value = "validationType", defaultValue = "") String validationType, RedirectAttributes redirectAttributes) {
        DomainConfig config = domainConfigs.getConfigForDomain(domain);
        if (config == null) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        InputStream stream = null;
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("validationTypes", getValidationTypes(config));
        try {
            if (fileManager.checkFileType(file.getInputStream())) {
                stream = file.getInputStream();
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
            attributes.put("message", "Provided invoice type is not valid");
        }
        try {
            if (stream != null) {
                XMLValidator validator = beans.getBean(XMLValidator.class, stream, validationType, config);
                TAR report = validator.validateAll();
//                attributes.put("title", config.getUploadTitle());
                attributes.put("config", config);
                attributes.put("report", report);
                attributes.put("date", report.getDate().toString());
                attributes.put("fileName", file.getOriginalFilename());
                // Cache detailed report.
                try {
                    String xmlID = fileManager.writeXML(config.getDomain(), report.getContext().getItem().get(0).getValue());
                    attributes.put("xmlID", xmlID);
                    fileManager.saveReport(report, xmlID);
                } catch (IOException e) {
                    logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                    attributes.put("message", "Error generating detailed report [" + e.getMessage() + "]");
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred during the invoice [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the invoice [" + e.getMessage() + "]");
        }
        return new ModelAndView("uploadForm", attributes);
    }

    public List<ValidationType> getValidationTypes(DomainConfig config) {
        List<ValidationType> types = new ArrayList<>();
        if (config.hasMultipleValidationTypes()) {
            for (String type: config.getType()) {
                types.add(new ValidationType(type, config.getTypeLabel().get(type)));
            }
        }
        return types;
    }
}
