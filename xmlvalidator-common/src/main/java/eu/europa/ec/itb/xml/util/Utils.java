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

package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.validation.commons.BomStrippingReader;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static eu.europa.ec.itb.validation.commons.Utils.secureXMLInputFactory;

/**
 * Class holding utility methods for common operations specific to the XML validator.
 */
public class Utils {

    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {}

    /**
     * Validate XML content using an XML Schema securely.
     * <p/>
     * Findings will be reported through the provided error handler which is also returned by this method.
     *
     * @param inputToValidate The input to validate.
     * @param schemaToValidateWith The schema to use.
     * @param errorHandler The error handler to configure (optional).
     * @param resourceResolver The resource resolver to configure (optional).
     * @param locale The locale to use for th parsing (optional).
     * @throws XMLStreamException If the input cannot be parsed as XML.
     * @throws SAXException If the input is invalid (not thrown for regular errors if a custom errorHandler is provided).
     */
    public static void secureSchemaValidation(InputStream inputToValidate, InputStream schemaToValidateWith, ErrorHandler errorHandler, LSResourceResolver resourceResolver, Locale locale) throws XMLStreamException, SAXException {
        /*
         * We create specifically a Xerces parser to allow localisation of output messages.
         * The security configuration for the Xerces parser involves:
         * - Setting the FEATURE_SECURE_PROCESSING to true.
         * - Using a secured underlying parser (see secureXMLInputFactory()) that completely disables DTD processing.
         * Xerces does not directly support the JAXP 1.5 features to disable XXE (ACCESS_EXTERNAL_DTD, ACCESS_EXTERNAL_SCHEMA)
         * but we ensure secure processing by means of the secured underlying parser.
         */
        XMLSchemaFactory factory = new XMLSchemaFactory();
        if (errorHandler != null) factory.setErrorHandler(errorHandler);
        if (resourceResolver != null) factory.setResourceResolver(resourceResolver);
        Schema schema;
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schema = factory.newSchema(new StreamSource(schemaToValidateWith));
        } catch (SAXException e) {
            throw new IllegalStateException("Unable to configure schema", e);
        }
        Validator validator = schema.newValidator();
        try {
            if (locale != null) validator.setProperty("http://apache.org/xml/properties/locale", locale);
            if (errorHandler != null) validator.setErrorHandler(errorHandler);
            if (resourceResolver != null) validator.setResourceResolver(resourceResolver);
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException("Unable to configure schema validator", e);
        }
        try (BomStrippingReader reader = new BomStrippingReader(inputToValidate)) {
            // If no custom error handler is set, the default implementation will throw an exception upon detected errors.
            validator.validate(new StAXSource(secureXMLInputFactory().createXMLStreamReader(reader)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read input stream", e);
        }
    }

}
