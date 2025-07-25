package eu.europa.ec.itb.xml.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UtilsTest {

    @Test
    void testSchemaValidationValid() {
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/valid.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, null, null, null);
            }
        });
    }

    @Test
    void testSchemaValidationInvalidXSD() throws SAXException {
        // Without error handler.
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xsd.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, null, null, null);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xsd.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, null, null);
            }
        });
        verify(errorHandler, atLeastOnce()).error(any(SAXParseException.class));
    }

    @Test
    void testSchemaValidationInvalidXML() throws SAXException {
        // Without error handler.
        assertThrows(IllegalStateException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xml.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, null, null, null);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xml.txt");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, null, null);
            }
        });
        verify(errorHandler, atLeastOnce()).error(any(SAXParseException.class));
    }

    @Test
    void testSchemaValidationMissingXML() throws SAXException {
        // Without error handler.
        assertThrows(IllegalStateException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/missing.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, null, null, null);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(IllegalStateException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/missing.txt");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, null, null);
            }
        });
        verify(errorHandler, never()).error(any());
    }

    @Test
    void testSchemaValidationInvalidXXE() throws SAXException {
        // Without error handler.
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xxe.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, null, null, null);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xxe.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                secureSchemaValidation(inputStream, schemaStream, errorHandler, null, null);
            }
        });
        verify(errorHandler, never()).error(any());
    }

}
