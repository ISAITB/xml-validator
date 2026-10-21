package eu.europa.ec.itb.xml.util;

import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.RemoteResourceBudget;
import eu.europa.ec.itb.validation.commons.URLReader;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the cache of remote resources (e.g. imported schemas).
 */
class FileManagerRemoteCacheTest {

    private static final int KB = 1024;

    @TempDir
    Path tmpFolder;
    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private FileManager fileManager;
    private ApplicationConfig appConfig;
    private DomainConfig domainConfig;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        for (var name: new String[] {"a", "b", "c", "large-ok"}) {
            server.createContext("/" + name + ".xsd", exchange -> {
                requests.incrementAndGet();
                exchange.sendResponseHeaders(200, 100 * KB);
                exchange.getResponseBody().write(new byte[100 * KB]);
                exchange.close();
            });
        }
        server.createContext("/large.xsd", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            try (var out = exchange.getResponseBody()) {
                out.write(new byte[64 * KB]);
                out.write(new byte[64 * KB]);
                out.write(new byte[64 * KB]);
            } catch (IOException e) {
                // The client stopped reading, as expected.
            }
        });
        server.start();
        appConfig = mock(ApplicationConfig.class);
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder.toString());
        var remoteLimits = new eu.europa.ec.itb.validation.commons.config.ApplicationConfig.RemoteLimits();
        remoteLimits.setCacheMaxSize(DataSize.ofKilobytes(250));
        when(appConfig.getRemoteLimits()).thenReturn(remoteLimits);
        domainConfig = mock(DomainConfig.class);
        when(domainConfig.getDomain()).thenReturn("domain");
        when(domainConfig.getHttpVersion()).thenReturn(HttpClient.Version.HTTP_1_1);
        fileManager = new FileManager();
        ReflectionTestUtils.setField(fileManager, "config", appConfig);
        ReflectionTestUtils.setField(fileManager, "urlReader", new URLReader());
        // Allow immediate eviction in the tests.
        fileManager.cacheEvictionGraceMillis = 0;
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private URI url(String name) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/" + name + ".xsd");
    }

    private long cachedFiles() throws IOException {
        var folder = tmpFolder.resolve("remote_cache").resolve("domain");
        if (!Files.exists(folder)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.count();
        }
    }

    @Test
    void testCachedResourceIsNotRequestedAgain() throws IOException {
        var first = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        var second = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        assertEquals(first, second);
        assertEquals(1, requests.get());
    }

    @Test
    void testLeastRecentlyUsedResourcesAreEvictedWhenCacheExceedsItsLimit() throws IOException {
        var a = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        var b = fileManager.retrieveCachedRemoteResource(domainConfig, url("b"), null, true);
        // Use "a" again so that "b" is the least recently used.
        fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        // The limit is 250KB so a third resource of 100KB requires an eviction.
        var c = fileManager.retrieveCachedRemoteResource(domainConfig, url("c"), null, true);
        assertTrue(Files.exists(a));
        assertFalse(Files.exists(b), "The least recently used resource must be evicted");
        assertTrue(Files.exists(c), "The resource just added must never be evicted");
        assertEquals(2, cachedFiles());
        // The evicted resource is downloaded again when needed.
        int before = requests.get();
        fileManager.retrieveCachedRemoteResource(domainConfig, url("b"), null, true);
        assertEquals(before + 1, requests.get());
    }

    @Test
    void testRecentlyUsedResourcesAreNotEvictedWithinTheGracePeriod() throws IOException {
        fileManager.cacheEvictionGraceMillis = 60000;
        var a = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        var b = fileManager.retrieveCachedRemoteResource(domainConfig, url("b"), null, true);
        fileManager.retrieveCachedRemoteResource(domainConfig, url("c"), null, true);
        // A resource that was just returned may still be read by a validation, so the limit is only enforced softly.
        assertTrue(Files.exists(a));
        assertTrue(Files.exists(b));
        assertEquals(3, cachedFiles());
    }

    @Test
    void testResourcesOfPreconfiguredSchemasAreNeverEvicted() throws IOException {
        // Pre-configured resources do not count towards the cache's size limit, nor are they ever evicted.
        var preconfigured = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, false);
        var b = fileManager.retrieveCachedRemoteResource(domainConfig, url("b"), null, true);
        var c = fileManager.retrieveCachedRemoteResource(domainConfig, url("c"), null, true);
        assertTrue(Files.exists(preconfigured), "A resource of a pre-configured schema must not be evicted");
        assertTrue(Files.exists(b));
        assertTrue(Files.exists(c));
        assertEquals(3, cachedFiles());
        // Neither does the use of user-provided resources evict it once their own limit is exceeded.
        var extra = fileManager.retrieveCachedRemoteResource(domainConfig, url("large-ok"), null, true);
        assertTrue(Files.exists(preconfigured));
        assertTrue(Files.exists(extra));
        assertFalse(Files.exists(b), "The least recently used user-provided resource must be evicted");
    }

    @Test
    void testResourceNeededByAPreconfiguredSchemaIsNoLongerEvicted() throws IOException {
        var a = fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, true);
        // Needed also by a pre-configured schema.
        assertEquals(a, fileManager.retrieveCachedRemoteResource(domainConfig, url("a"), null, false));
        fileManager.retrieveCachedRemoteResource(domainConfig, url("b"), null, true);
        fileManager.retrieveCachedRemoteResource(domainConfig, url("c"), null, true);
        fileManager.retrieveCachedRemoteResource(domainConfig, url("large-ok"), null, true);
        assertTrue(Files.exists(a), "A resource used by a pre-configured schema must not be evicted");
    }

    @Test
    void testPartialDownloadIsNotLeftInTheCache() throws IOException {
        var limits = new eu.europa.ec.itb.validation.commons.config.ApplicationConfig.RemoteLimits();
        limits.setMaxResourceSize(DataSize.ofKilobytes(100));
        limits.setRequestTimeout(30000);
        try (var ignored = RemoteResourceBudget.open(limits)) {
            assertThrows(ValidatorException.class, () -> fileManager.retrieveCachedRemoteResource(domainConfig, url("large"), null, true));
        }
        assertEquals(0, cachedFiles());
    }

}
