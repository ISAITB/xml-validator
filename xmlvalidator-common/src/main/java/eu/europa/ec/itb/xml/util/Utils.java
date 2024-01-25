package eu.europa.ec.itb.xml.util;

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
     * Validate XML content using an XML Schema securely.
     * <p/>
     * Findings will be reported through the provided error handler which is also returned by this method.
     *
     * @param inputToValidate The input to validate.
     * @param schemaToValidateWith The schema to use.
     * @param errorHandler The error handler to configure (optional).
     * @param resourceResolver The resource resolver to configure (optional).
     * @param locale The locale to use for th parsing (optional).
     */
    public static void secureSchemaValidation(InputStream inputToValidate, InputStream schemaToValidateWith, ErrorHandler errorHandler, LSResourceResolver resourceResolver, Locale locale) {
        /*
         * We create specifically a Xerces parser to allow localisation of output messages.
         * The security configuration for the Xerces parser involves:
         * - Setting the FEATURE_SECURE_PROCESSING to true.
         * - Using a secured underlying parser (see secureXMLInputFactory()) that completely disables DTD processing.
         * Xerces does not directly support the JAXP 1.5 features to disable XXE (ACCESS_EXTERNAL_DTD, ACCESS_EXTERNAL_SCHEMA)
         * but we ensure secure processing by means of the secured underlying parser.
         */
        XMLSchemaFactory factory = new XMLSchemaFactory();
        if (errorHandler != null) {
            factory.setErrorHandler(errorHandler);
        }
        if (resourceResolver != null) {
            factory.setResourceResolver(resourceResolver);
        }
        Schema schema;
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schema = factory.newSchema(new StreamSource(schemaToValidateWith));
        } catch (SAXException e) {
            throw new IllegalStateException("Unable to configure schema", e);
        }
        Validator validator = schema.newValidator();
        try {
            if (locale != null) {
                validator.setProperty("http://apache.org/xml/properties/locale", locale);
            }
            validator.setErrorHandler(factory.getErrorHandler());
            validator.setResourceResolver(factory.getResourceResolver());
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException("Unable to configure schema validator", e);
        }
        try {
            validator.validate(new StAXSource(secureXMLInputFactory().createXMLStreamReader(inputToValidate)));
        } catch (SAXException | IOException | XMLStreamException e) {
            throw new IllegalStateException("Unable to validate input", e);
        }
    }

}
