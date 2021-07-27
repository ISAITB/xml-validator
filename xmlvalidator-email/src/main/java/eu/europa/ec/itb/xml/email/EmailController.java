package eu.europa.ec.itb.xml.email;

import eu.europa.ec.itb.xml.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller used to trigger the checking of emails.
 */
@RestController
@Profile("email")
public class EmailController {

    @Autowired
    MailHandler mailHandler;

    @Autowired
    ApplicationConfig config;

    /**
     * Check for new emails.
     *
     * @return A confirmation text.
     */
    @RequestMapping(value = "/email/read", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String readEmail() {
        mailHandler.receiveEmail();
        return "Email read";
    }

}
