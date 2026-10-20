package eu.europa.ec.itb.xml.validation;

import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.xml.ApplicationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.europa.ec.itb.xml.util.Utils.secureSaxSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests that content processed by the XSLT and XPath preprocessing is parsed securely, i.e. that DOCTYPE declarations
 * (and with them external entities) are not processed.
 */
class XSLTPreprocessorTest {

    private static final String IDENTITY_XSLT = """
            <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:template match="@*|node()"><xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy></xsl:template>
            </xsl:stylesheet>
            """;

    @TempDir
    Path root;
    private HttpServer server;
    private final AtomicInteger entityRequests = new AtomicInteger();
    private XSLTPreprocessor preprocessor;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/entity", exchange -> {
            entityRequests.incrementAndGet();
            byte[] bytes = "entity-content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        preprocessor = new XSLTPreprocessor();
        ReflectionTestUtils.setField(preprocessor, "config", mock(ApplicationConfig.class));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String documentWithExternalEntity() {
        return "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY ext SYSTEM \"http://127.0.0.1:%d/entity\">]><root>&ext;</root>".formatted(server.getAddress().getPort());
    }

    @Test
    void testPreprocessingOfRegularContent() throws Exception {
        var xslt = Files.writeString(root.resolve("identity.xsl"), IDENTITY_XSLT);
        var input = Files.writeString(root.resolve("input.xml"), "<root><item>value</item></root>");
        var output = preprocessor.preprocessFile(input.toFile(), xslt.toFile(), "xml");
        assertTrue(Files.readString(output.toPath()).contains("<item>value</item>"));
    }

    @Test
    void testPreprocessingDoesNotProcessExternalEntities() throws Exception {
        var xslt = Files.writeString(root.resolve("identity.xsl"), IDENTITY_XSLT);
        var input = Files.writeString(root.resolve("input.xml"), documentWithExternalEntity());
        assertThrows(Exception.class, () -> preprocessor.preprocessFile(input.toFile(), xslt.toFile(), "xml"));
        assertEquals(0, entityRequests.get(), "The external entity must not be requested");
    }

    @Test
    void testXPathPreprocessingOfRegularContent() throws Exception {
        var xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath().compile("/root/item");
        try (var in = new java.io.ByteArrayInputStream("<root><item>value</item></root>".getBytes(StandardCharsets.UTF_8))) {
            var result = xPath.evaluate(secureSaxSource(in), XPathConstants.NODE);
            assertInstanceOf(net.sf.saxon.om.NodeInfo.class, result);
            assertEquals("value", ((net.sf.saxon.om.NodeInfo) result).getStringValue());
        }
    }

    @Test
    void testXPathPreprocessingDoesNotProcessExternalEntities() throws Exception {
        var xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath().compile("/root");
        try (var in = new java.io.ByteArrayInputStream(documentWithExternalEntity().getBytes(StandardCharsets.UTF_8))) {
            assertThrows(Exception.class, () -> xPath.evaluate(secureSaxSource(in), XPathConstants.NODE));
        }
        assertEquals(0, entityRequests.get(), "The external entity must not be requested");
    }

}
