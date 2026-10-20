package eu.europa.ec.itb.xml.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the schema location hints in the XML being validated are ignored (so that the XML cannot cause the
 * validator to load schemas, or make requests, of its choosing).
 */
class SchemaLocationHintTest {

    private static final String SCHEMA = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns="urn:test" targetNamespace="urn:test" elementFormDefault="qualified">
              <xs:element name="root">
                <xs:complexType>
                  <xs:sequence>
                    <xs:any namespace="##other" processContents="strict"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

    @TempDir
    Path root;
    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] bytes = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:other\"><xs:element name=\"other\" type=\"xs:string\"/></xs:schema>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void testSchemaLocationHintsInInputAreNotFollowed() throws Exception {
        Path schema = Files.writeString(root.resolve("schema.xsd"), SCHEMA);
        String input = "<root xmlns=\"urn:test\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:o=\"urn:other\" xsi:schemaLocation=\"urn:other http://127.0.0.1:%d/other.xsd\"><o:other>x</o:other></root>"
                .formatted(server.getAddress().getPort());
        List<String> errors = new ArrayList<>();
        ErrorHandler handler = new ErrorHandler() {
            @Override public void warning(SAXParseException e) { /* ignore */ }
            @Override public void error(SAXParseException e) { errors.add(e.getMessage()); }
            @Override public void fatalError(SAXParseException e) { errors.add(e.getMessage()); }
        };
        Utils.secureSchemaValidation(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), schema, handler, null, Locale.ENGLISH, null);
        assertEquals(0, requests.get(), "The schema location hint must not be requested");
        assertFalse(errors.isEmpty(), "The content of the unknown namespace cannot be validated without its schema");
    }

}
