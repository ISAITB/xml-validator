package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
@Profile("form")
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    FileController fileController;

    @Autowired
    BeanFactory beans;

    @Autowired
    ApplicationConfig config;
    private List validationTypes;

    @RequestMapping(method = RequestMethod.GET, value = "/upload")
    public ModelAndView upload(Model model) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("title", config.getUploadTitle());
        attributes.put("validationTypes", getValidationTypes());
        return new ModelAndView("uploadForm", attributes);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/upload")
    public ModelAndView handleUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "validationType", defaultValue = "") String validationType, RedirectAttributes redirectAttributes) {
        InputStream stream = null;
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("validationTypes", getValidationTypes());
        try {
            if (fileController.checkFileType(file.getInputStream())) {
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
        if (config.getType().size() > 1 && (validationType == null || !config.getType().contains(validationType))) {
            // A validation type is required.
            attributes.put("message", "Provided validation type is not valid");
        }
        try {
            if (stream != null) {
                XMLValidator validator = beans.getBean(XMLValidator.class, stream, validationType);
                TAR report = validator.validateAll();
                attributes.put("title", config.getUploadTitle());
                attributes.put("report", report);
                attributes.put("date", report.getDate().toString());
                attributes.put("fileName", file.getOriginalFilename());
                // Cache detailed report.
                try {
                    String xmlID = fileController.writeXML(report.getContext().getItem().get(0).getValue());
                    attributes.put("xmlID", xmlID);
                    fileController.saveReport(report, xmlID);
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

    public List<ValidationType> getValidationTypes() {
        List<ValidationType> types = new ArrayList<>();
        if (config.getType().size() > 1) {
            for (String type: config.getType()) {
                types.add(new ValidationType(type, config.getTypeLabel().get(type)));
            }
        }
        return types;
    }
}
