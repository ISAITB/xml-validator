package eu.europa.ec.itb.xml.validation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
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
 * Tests for the loading of remote resources referenced from Schematron files (via the URI resolver and the include resolver).
 */
class SchematronURIResolverTest {

    @TempDir
    Path root;
    private Path tmpFolder;
    private Path schematron;
    private HttpServer server;
    private final AtomicInteger blockedTargetRequests = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        tmpFolder = Files.createDirectories(root.resolve("tmp"));
        schematron = tmpFolder.resolve("main.sch");
        Files.writeString(schematron, "<sch:schema xmlns:sch=\"http://purl.oclc.org/dsdl/schematron\"/>");
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/allowed/content.xml", exchange -> respond(exchange, 200, "<data>allowed</data>", null));
        server.createContext("/allowed/redirect.xml", exchange -> respond(exchange, 302, "", "/allowed/content.xml"));
        server.createContext("/allowed/to-blocked.xml", exchange -> respond(exchange, 302, "", "/blocked/content.xml"));
        server.createContext("/blocked/content.xml", exchange -> {
            blockedTargetRequests.incrementAndGet();
            respond(exchange, 200, "<data>blocked</data>", null);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body, String location) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String serverUrl(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
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

    private SchematronURIResolver uriResolver(String... allowedUriPrefixes) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder.toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(java.util.Arrays.stream(allowedUriPrefixes).map(prefix -> NormalizedURI.of(URI.create(prefix))).toList());
        var domainConfig = mock(DomainConfig.class);
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        var resolver = new SchematronURIResolver(schematron.toFile(), ImportedUriAuthorizer.forUserContent(appConfig), ImportedFileAuthorizer.from(appConfig, domainConfig));
        resolver.setHttpVersion(HttpClient.Version.HTTP_1_1);
        resolver.setAllowedRemoteSchemes("http", "https");
        return resolver;
    }

    private String read(javax.xml.transform.Source source) throws IOException {
        try (var in = ((StreamSource) source).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testRemoteResourceIsLoadedIfAllowed() throws Exception {
        var resolver = uriResolver(serverUrl("/allowed"));
        assertEquals("<data>allowed</data>", read(resolver.resolve(serverUrl("/allowed/content.xml"), "")));
    }

    @Test
    void testRedirectToAllowedLocationIsFollowed() throws Exception {
        var resolver = uriResolver(serverUrl("/allowed"));
        assertEquals("<data>allowed</data>", read(resolver.resolve(serverUrl("/allowed/redirect.xml"), "")));
    }

    @Test
    void testRedirectToLocationThatIsNotAllowedIsRejected() {
        var resolver = uriResolver(serverUrl("/allowed"));
        assertThrows(Exception.class, () -> resolver.resolve(serverUrl("/allowed/to-blocked.xml"), ""));
        assertEquals(0, blockedTargetRequests.get(), "The redirect target must not be requested");
    }

    @Test
    void testRemoteResourceThatIsNotAllowedIsRejected() {
        var resolver = uriResolver();
        assertThrows(Exception.class, () -> resolver.resolve(serverUrl("/blocked/content.xml"), ""));
        assertEquals(0, blockedTargetRequests.get());
    }

    @Test
    void testRemoteIncludeIsDownloadedAndCleanedUp() throws Exception {
        var resolver = new SchematronIncludeResolver(schematron.toFile(), authorizer(serverUrl("/allowed")), new eu.europa.ec.itb.validation.commons.URLReader(), HttpClient.Version.HTTP_1_1);
        var resource = resolver.getResolvedSchematronResource(serverUrl("/allowed/redirect.xml"));
        var downloaded = resource.getAsFile();
        assertNotNull(downloaded);
        assertEquals("<data>allowed</data>", Files.readString(downloaded.toPath()));
        assertEquals(tmpFolder.toRealPath(), downloaded.toPath().getParent().toRealPath());
        resolver.cleanup();
        assertFalse(downloaded.exists());
    }

    @Test
    void testRemoteIncludeRedirectToLocationThatIsNotAllowedIsRejected() {
        var resolver = new SchematronIncludeResolver(schematron.toFile(), authorizer(serverUrl("/allowed")), new eu.europa.ec.itb.validation.commons.URLReader(), HttpClient.Version.HTTP_1_1);
        assertThrows(Exception.class, () -> resolver.getResolvedSchematronResource(serverUrl("/allowed/to-blocked.xml")));
        assertEquals(0, blockedTargetRequests.get(), "The redirect target must not be requested");
        assertEquals(List.of(), List.of(tmpFolder.toFile().list((dir, name) -> name.startsWith("include-"))));
    }

}
