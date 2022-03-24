package eu.europa.ec.itb.xml.email;

import eu.europa.ec.itb.xml.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping(value = "/email/read", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String readEmail() {
        mailHandler.receiveEmail();
        return "Email read";
    }

}
