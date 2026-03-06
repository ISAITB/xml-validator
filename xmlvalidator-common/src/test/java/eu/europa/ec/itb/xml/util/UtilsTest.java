package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.xml.XmlSchemaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UtilsTest {

    @TempDir
    Path tempDirectory;

    @Test
    void testSchemaValidationValid() {
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/valid.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_0);
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
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_0);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xsd.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, errorHandler, null, null, XmlSchemaVersion.VERSION_1_0);
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
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_0);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xml.txt");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, errorHandler, null, null, XmlSchemaVersion.VERSION_1_0);
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
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_0);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(IllegalStateException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/missing.txt");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, errorHandler, null, null, XmlSchemaVersion.VERSION_1_0);
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
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_0);
            }
        });
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertThrows(SAXException.class, () -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xxe.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, errorHandler, null, null, XmlSchemaVersion.VERSION_1_0);
            }
        });
        verify(errorHandler, never()).error(any());
    }

    @Test
    void testSchemaValidationWithVersionDetection() {
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/valid.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/PurchaseOrder.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() +".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, null);
            }
        });
    }
}
