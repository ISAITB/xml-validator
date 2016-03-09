package eu.europa.ec.itb.einvoice.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by simatosc on 07/03/2016.
 */
@Controller
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    FileController fileController;

    @RequestMapping(method = RequestMethod.GET, value = "/upload")
    public String upload(Model model) {
        return "uploadForm";
    }

    @RequestMapping(method = RequestMethod.POST, value = "/upload")
    public ModelAndView handleUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        InputStream stream = null;
        try {
            stream = file.getInputStream();
        } catch (IOException e) {
            logger.error("Error while reading uploaded file ["+e.getMessage()+"]", e);
            redirectAttributes.addFlashAttribute("message", "Error in upload [" + e.getMessage() + "]");
        }
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (stream != null) {
            XMLValidator validator = new XMLValidator(stream);
            TAR report = validator.validateAll();
            attributes.put("message", "Validation result [" + report.getResult() + "]");
            attributes.put("time", new Date());
            attributes.put("report", report);
            attributes.put("date", report.getDate().toString());
            // Cache detailed report.
            try {
                String xmlID = fileController.writeXML(report.getContext().getItem().get(0).getValue());
                attributes.put("xmlID", xmlID);
                fileController.saveReport(report, xmlID);
            } catch (IOException e) {
                logger.error("Error generating detailed report ["+e.getMessage()+"]", e);
                redirectAttributes.addFlashAttribute("message", "Error generating detailed report [" + e.getMessage() + "]");
            }
        }
        return new ModelAndView("uploadForm", attributes);
    }

}
