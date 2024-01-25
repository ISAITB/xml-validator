package eu.europa.ec.itb.xml.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilsTest {

    @Test
    void testSecureSchemaValidator() throws SAXException {
        var errorHandler = mock(ErrorHandler.class);
        var resourceResolver = mock(LSResourceResolver.class);
        // Test to ensure XXE is blocked.
        String xmlToReject = """
<?xml version="1.0"?>
<!DOCTYPE replace [<!ENTITY xxe "Attack"> ]>
<foo>
  <bar>&xxe;</bar>
</foo>
                """;
        assertThrows(IllegalStateException.class, () -> {
            try (
                    var inputStream = new ByteArrayInputStream(xmlToReject.getBytes(StandardCharsets.UTF_8));
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, resourceResolver, Locale.FRENCH);
            }
        });
        // Test to ensure non-XXE content is not blocked.
        String xmlToParse = """
<?xml version="1.0"?>
<foo>
  <bar>TEXT</bar>
</foo>
                """;
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = new ByteArrayInputStream(xmlToParse.getBytes(StandardCharsets.UTF_8));
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, resourceResolver, Locale.FRENCH);
            }
        });
        verify(errorHandler, atLeastOnce()).error(any(SAXParseException.class));
    }

}
