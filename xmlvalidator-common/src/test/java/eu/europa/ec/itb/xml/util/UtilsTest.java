package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.xml.XmlSchemaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static eu.europa.ec.itb.validation.commons.Utils.secureTransformer;
import static eu.europa.ec.itb.xml.util.Utils.secureSaxSource;
import static eu.europa.ec.itb.xml.util.Utils.secureSchemaValidation;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void testSchemaValidationXsd11CtaFullXPathValid() {
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/valid_cta.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/CtaXPath.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() + ".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, null, null, null, XmlSchemaVersion.VERSION_1_1);
            }
        });
    }

    @Test
    void testSchemaValidationXsd11CtaFullXPathInvalid() throws SAXException {
        // With error handler.
        var errorHandler = mock(ErrorHandler.class);
        assertDoesNotThrow(() -> {
            try (
                    var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_cta.xml");
                    var schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/CtaXPath.xsd")
            ) {
                Path schemaPath = tempDirectory.resolve(UUID.randomUUID() + ".xsd");
                Files.copy(Objects.requireNonNull(schemaStream), schemaPath);
                secureSchemaValidation(inputStream, schemaPath, errorHandler, null, null, XmlSchemaVersion.VERSION_1_1);
            }
        });
        verify(errorHandler, atLeastOnce()).error(any(SAXParseException.class));
    }

    @Test
    void testSecureSaxSourceParsesNamespacedContent() throws TransformerException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns:root xmlns:ns="urn:test:namespace">
                    <ns:child>value</ns:child>
                </ns:root>
                """;
        var result = new DOMResult();
        secureTransformer().transform(secureSaxSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), result);
        var document = (org.w3c.dom.Document) result.getNode();
        var root = document.getDocumentElement();
        assertEquals("urn:test:namespace", root.getNamespaceURI());
        assertEquals("root", root.getLocalName());
        assertEquals("child", root.getFirstChild().getNextSibling().getLocalName());
    }

    @Test
    void testSecureSaxSourceRejectsDoctype() {
        assertThrows(TransformerException.class, () -> {
            try (var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("utils/testFiles/invalid_xxe.xml")) {
                secureTransformer().transform(secureSaxSource(inputStream), new DOMResult());
            }
        });
    }
}
