package eu.europa.ec.itb.xml.validation;

import com.helger.io.resource.FileSystemResource;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the restriction of the resources that XSLT-based Schematron can read as text, i.e. via the XPath functions
 * {@code unparsed-text()}, {@code unparsed-text-lines()}, {@code unparsed-text-available()} and {@code json-doc()}.
 * <p>
 * These are covered by the URI resolver set for the Schematron processing (Saxon resolves them via its resource resolver, which
 * is where the URI resolver is installed). The tests make sure this remains the case.
 */
class SchematronTextResourceRestrictionTest {

    @TempDir
    Path root;
    private Path tmpFolder;
    private Path outsideFolder;
    private ImportedUriAuthorizer uriAuthorizer;
    private ImportedFileAuthorizer fileAuthorizer;
    private HttpServer server;
    private final AtomicInteger internalRequests = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        tmpFolder = Files.createDirectories(root.resolve("tmp"));
        outsideFolder = Files.createDirectories(root.resolve("outside"));
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder.toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of());
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        uriAuthorizer = ImportedUriAuthorizer.forUserContent(appConfig);
        fileAuthorizer = ImportedFileAuthorizer.from(appConfig, domainConfig);
        // An "internal" service (loopback is internal and not whitelisted).
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/secret", exchange -> {
            internalRequests.incrementAndGet();
            byte[] bytes = "{\"secret\":\"internal\"}".getBytes(StandardCharsets.UTF_8);
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

    /**
     * Run an XSLT-based Schematron, set up in the same way as in the validator, that reports the result of the provided XPath expression.
     */
    private String runSchematron(String expression) throws Exception {
        var xslt = tmpFolder.resolve("schematron.xsl");
        Files.writeString(xslt, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:svrl="http://purl.oclc.org/dsdl/svrl">
                  <xsl:template match="/">
                    <svrl:schematron-output title="test">
                      <svrl:failed-assert test="x" location="/">
                        <svrl:text>FOUND:<xsl:value-of select="%s"/></svrl:text>
                      </svrl:failed-assert>
                    </svrl:schematron-output>
                  </xsl:template>
                </xsl:stylesheet>
                """.formatted(expression.replace("&", "&amp;").replace("<", "&lt;")));
        var resolver = new SchematronURIResolver(xslt.toFile(), uriAuthorizer, fileAuthorizer);
        resolver.setAllowedRemoteSchemes("http", "https");
        var schematron = SchematronResourceXSLT.builder(new FileSystemResource(xslt.toFile()))
                .uriResolver(resolver)
                .validateSVRL(false)
                .useCache(false)
                .transformerFactoryCustomizer(factory -> SchematronCollectionAuthorizer.install(factory, uriAuthorizer, fileAuthorizer))
                .build();
        var output = schematron.applySchematronValidationToSVRL(new StreamSource(new StringReader("<root/>")));
        assertNotNull(output);
        return SVRLHelper.getAllFailedAssertions(output).get(0).getText();
    }

    @Test
    void testUnparsedTextOfFileOutsideAllowedFoldersIsBlocked() throws Exception {
        var secret = outsideFolder.resolve("secret.txt");
        Files.writeString(secret, "outside-content");
        assertThrows(Exception.class, () -> runSchematron("unparsed-text('" + secret.toUri() + "')"));
        assertThrows(Exception.class, () -> runSchematron("string-join(unparsed-text-lines('" + secret.toUri() + "'), ',')"));
        // unparsed-text-available() must not be usable to probe for files either.
        assertThrows(Exception.class, () -> runSchematron("unparsed-text-available('" + secret.toUri() + "')"));
    }

    @Test
    void testJsonDocOfFileOutsideAllowedFoldersIsBlocked() throws Exception {
        var secret = outsideFolder.resolve("secret.json");
        Files.writeString(secret, "{\"secret\":\"outside\"}");
        assertThrows(Exception.class, () -> runSchematron("json-doc('" + secret.toUri() + "')?secret"));
    }

    @Test
    void testUnparsedTextOfInternalUriIsBlockedWithoutRequest() throws Exception {
        var uri = "http://127.0.0.1:" + server.getAddress().getPort() + "/secret";
        assertThrows(Exception.class, () -> runSchematron("unparsed-text('" + uri + "')"));
        assertThrows(Exception.class, () -> runSchematron("json-doc('" + uri + "')?secret"));
        assertEquals(0, internalRequests.get(), "The internal service must not be requested");
    }

    @Test
    void testUnparsedTextOfFileInAllowedFolderIsAllowed() throws Exception {
        var data = tmpFolder.resolve("data.txt");
        Files.writeString(data, "inside-content");
        assertEquals("FOUND:inside-content", runSchematron("unparsed-text('" + data.toUri() + "')").trim());
        assertEquals("FOUND:true", runSchematron("unparsed-text-available('" + data.toUri() + "')").trim());
    }

    @Test
    void testJsonDocOfFileInAllowedFolderIsAllowed() throws Exception {
        Files.writeString(tmpFolder.resolve("data.json"), "{\"value\":\"inside\"}");
        assertEquals("FOUND:inside", runSchematron("json-doc('" + tmpFolder.resolve("data.json").toUri() + "')?value").trim());
    }

    @Test
    void testTrustedProcessingWithoutAuthorizersIsUnchanged() throws Exception {
        var xslt = tmpFolder.resolve("trusted.xsl");
        var data = outsideFolder.resolve("trusted.txt");
        Files.writeString(data, "trusted-content");
        Files.writeString(xslt, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:svrl="http://purl.oclc.org/dsdl/svrl">
                  <xsl:template match="/">
                    <svrl:schematron-output title="test">
                      <svrl:failed-assert test="x" location="/">
                        <svrl:text>FOUND:<xsl:value-of select="unparsed-text('%s')"/></svrl:text>
                      </svrl:failed-assert>
                    </svrl:schematron-output>
                  </xsl:template>
                </xsl:stylesheet>
                """.formatted(data.toUri()));
        var schematron = SchematronResourceXSLT.builder(new FileSystemResource(xslt.toFile()))
                .validateSVRL(false)
                .useCache(false)
                .build();
        var output = schematron.applySchematronValidationToSVRL(new StreamSource(new StringReader("<root/>")));
        assertEquals("FOUND:trusted-content", SVRLHelper.getAllFailedAssertions(output).get(0).getText().trim());
    }

}
