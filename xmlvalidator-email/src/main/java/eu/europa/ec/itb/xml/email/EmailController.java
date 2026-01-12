/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
