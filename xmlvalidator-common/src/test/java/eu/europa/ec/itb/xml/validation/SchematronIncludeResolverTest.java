package eu.europa.ec.itb.xml.validation;

import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.URLResource;
import com.helger.schematron.CSchematron;
import com.helger.schematron.SchematronHelper;
import com.helger.schematron.pure.SchematronResourcePureXPath;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.SAXReaderSettings;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the checking of the includes of "pure" Schematron files.
 */
class SchematronIncludeResolverTest {

    private static final String RULES = """
            <sch:pattern xmlns:sch="http://purl.oclc.org/dsdl/schematron">
                <sch:rule context="/invoice">
                    <sch:assert test="total">Invoice must have a total element</sch:assert>
                </sch:rule>
            </sch:pattern>
            """;

    @TempDir
    Path root;
    private Path tmpFolder;
    private Path requestFolder;
    private Path schematron;

    @BeforeEach
    void setUp() throws Exception {
        tmpFolder = Files.createDirectories(root.resolve("tmp"));
        requestFolder = Files.createDirectories(tmpFolder.resolve("request"));
        schematron = requestFolder.resolve("main.sch");
        Files.writeString(schematron, "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"/>");
    }

    private SchematronReferenceAuthorizer authorizer(String... allowedUriPrefixes) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder.toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(java.util.Arrays.stream(allowedUriPrefixes).map(prefix -> NormalizedURI.of(URI.create(prefix))).toList());
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        var artifactInfo = mock(TypedValidationArtifactInfo.class);
        when(artifactInfo.getOverallExternalArtifactSupport()).thenReturn(ExternalArtifactSupport.OPTIONAL);
        when(domainConfig.getArtifactInfo()).thenReturn(Map.of("type1", artifactInfo));
        return new SchematronReferenceAuthorizer(
                ImportedUriAuthorizer.from(appConfig, domainConfig, "type1").orElseThrow(),
                ImportedFileAuthorizer.from(appConfig, domainConfig)
        );
    }

    private SchematronIncludeResolver resolver(String... allowedUriPrefixes) {
        return new SchematronIncludeResolver(schematron.toFile(), authorizer(allowedUriPrefixes));
    }

    @Test
    void testRelativeIncludeNextToSchematronIsAllowed() throws Exception {
        var resolver = resolver();
        assertFalse(resolver.hasResolvedIncludes());
        var resource = resolver.getResolvedSchematronResource("rules.sch");
        assertTrue(resolver.hasResolvedIncludes());
        assertInstanceOf(FileSystemResource.class, resource);
        assertEquals(requestFolder.resolve("rules.sch").toFile().getCanonicalFile(), resource.getAsFile().getCanonicalFile());
    }

    @Test
    void testIncludesOutsideAllowedFoldersAreRejected() {
        var resolver = resolver();
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource("../../outside.sch"));
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource(root.resolve("outside.sch").toUri().toString()));
    }

    @Test
    void testNonWhitelistedInternalUriIsRejected() {
        var resolver = resolver("http://127.0.0.1:9/allowed/");
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource("http://127.0.0.1:9/private/rules.sch"));
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource("https://localhost/rules.sch"));
    }

    @Test
    void testWhitelistedInternalUriIsAllowed() throws Exception {
        var resolver = resolver("http://127.0.0.1:9/allowed/");
        var resource = resolver.getResolvedSchematronResource("http://127.0.0.1:9/allowed/rules.sch");
        assertInstanceOf(URLResource.class, resource);
    }

    @Test
    void testOtherSchemesAreRejected() {
        var resolver = resolver("http://127.0.0.1:9/allowed/");
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource("ftp://127.0.0.1/rules.sch"));
        assertThrows(ValidatorException.class, () -> resolver.getResolvedSchematronResource("jar:" + root.resolve("a.zip").toUri() + "!/rules.sch"));
    }

    @Test
    void testIncludesAreResolvedAndAppliedByPureEngine() throws Exception {
        Files.writeString(requestFolder.resolve("rules.sch"), RULES);
        Files.writeString(schematron, "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"><sch:include href=\"rules.sch\"/></sch:schema>");
        var resolved = resolveAndValidate(resolver(), "<invoice><currency>EUR</currency></invoice>");
        assertEquals(1, resolved.size());
        assertEquals("Invoice must have a total element", resolved.get(0).getText());
    }

    @Test
    void testWhitelistedRemoteIncludeIsFetchedAndNonWhitelistedIsNot() throws Exception {
        var hits = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            byte[] body = RULES.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            Files.writeString(schematron, "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"><sch:include href=\"%s/private/rules.sch\"/></sch:schema>".formatted(base));
            // Not whitelisted: rejected without any request.
            assertThrows(ValidatorException.class, () -> resolveAndValidate(resolver(), "<invoice/>"));
            assertEquals(0, hits.get());
            // Whitelisted: fetched and applied.
            var findings = resolveAndValidate(resolver(base + "/private/"), "<invoice/>");
            assertTrue(hits.get() > 0);
            assertEquals(1, findings.size());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Resolve the includes of the test schematron (as done before processing it as "pure" Schematron) and validate
     * the provided XML with the result.
     */
    private List<com.helger.schematron.svrl.AbstractSVRLMessage> resolveAndValidate(SchematronIncludeResolver resolver, String xml) throws Exception {
        var errorHandler = new PureSchematronErrorHandler();
        var document = SchematronHelper.getWithResolvedSchematronIncludes(new FileSystemResource(schematron.toFile()), new SAXReaderSettings(), errorHandler, resolver, CSchematron.DEFAULT_ALLOW_DEPRECATED_NAMESPACES);
        assertNotNull(document);
        var resolvedFile = Files.writeString(requestFolder.resolve("resolved.sch"), MicroWriter.getNodeAsString(document));
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var input = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        var output = SchematronResourcePureXPath.builderFromFile(resolvedFile.toFile()).errorHandler(errorHandler).useCache(false).build()
                .applySchematronValidationToSVRL(new javax.xml.transform.dom.DOMSource(input));
        assertNotNull(output);
        return List.copyOf(SVRLHelper.getAllFailedAssertions(output));
    }

}
