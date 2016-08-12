package eu.europa.ec.itb.einvoice.email;

import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Created by simatosc on 16/03/2016.
 */
@RestController
@Profile("email")
public class EmailController {

    @Autowired
    MailHandler mailHandler;

    @Autowired
    ApplicationConfig config;

    @RequestMapping(value = "/email/read", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String readEmail() {
        mailHandler.receiveEmail();
        return "Email read";
    }

}
