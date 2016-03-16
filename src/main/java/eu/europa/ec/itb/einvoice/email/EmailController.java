package eu.europa.ec.itb.einvoice.email;

import eu.europa.ec.itb.einvoice.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * Created by simatosc on 16/03/2016.
 */
@RestController
public class EmailController {

    @Autowired
    MailHandler mailHandler;

    @RequestMapping(value = "/email/read", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String readEmail() {
        mailHandler.receiveEmail();
        return "Email read";
    }

}
