package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.RemoteRequestGuard;
import eu.europa.ec.itb.validation.commons.StreamInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for the restrictions applied when schemas are imported remotely.
 */
class XSDFileResolverTest {

    private static final String SCHEMA_SOURCE = "https://93.184.216.35/schemas/main.xsd";

    @TempDir
    Path root;
    private FileManager fileManager;
    private DomainConfig domainConfig;
    private ImportedUriAuthorizer uriAuthorizer;
    private ImportedFileAuthorizer fileAuthorizer;

    @BeforeEach
    void setUp() {
        fileManager = mock(FileManager.class);
        domainConfig = mock(DomainConfig.class);
        when(domainConfig.getHttpVersion()).thenReturn(HttpClient.Version.HTTP_1_1);
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(true);
        when(appConfig.getTmpFolder()).thenReturn(root.resolve("tmp").toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of(NormalizedURI.of(URI.create("http://10.0.0.1/allowed"))));
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        uriAuthorizer = ImportedUriAuthorizer.forUserContent(appConfig);
        fileAuthorizer = ImportedFileAuthorizer.from(appConfig, domainConfig);
    }

    private XSDFileResolver resolver(boolean skipCaching, Consumer<java.net.http.HttpRequest.Builder> decorator) {
        when(domainConfig.isSkipRemoteSchemaImportCaching()).thenReturn(skipCaching);
        var resolver = new XSDFileResolver(domainConfig, URI.create(SCHEMA_SOURCE), decorator, uriAuthorizer, fileAuthorizer);
        ReflectionTestUtils.setField(resolver, "fileManager", fileManager);
        return resolver;
    }

    @Test
    void testRemoteImportIsLoadedWithGuard() throws Exception {
        when(fileManager.getInputStreamFromURL(any(), any(), any(), any(), any(RemoteRequestGuard.class)))
                .thenReturn(new StreamInfo(new ByteArrayInputStream("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>".getBytes()), Optional.empty()));
        Consumer<java.net.http.HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret");
        var input = resolver(true, decorator).resolveResource("http://www.w3.org/2001/XMLSchema", null, null, "https://93.184.216.34/imported.xsd", SCHEMA_SOURCE);
        assertNotNull(input);
        var guardCaptor = ArgumentCaptor.forClass(RemoteRequestGuard.class);
        verify(fileManager).getInputStreamFromURL(eq("https://93.184.216.34/imported.xsd"), any(), any(), eq(decorator), guardCaptor.capture());
        var guard = guardCaptor.getValue();
        // Every request (including redirect targets) is checked.
        assertDoesNotThrow(() -> guard.check(URI.create("https://93.184.216.34/other.xsd")));
        assertDoesNotThrow(() -> guard.check(URI.create("http://10.0.0.1/allowed/other.xsd")));
        assertThrows(ValidatorException.class, () -> guard.check(URI.create("http://169.254.169.254/latest/meta-data/")));
        assertThrows(ValidatorException.class, () -> guard.check(URI.create("http://127.0.0.1:8080/internal")));
        // Credentials are only sent to the host the schema itself comes from.
        assertTrue(guard.mayDecorate(URI.create("https://93.184.216.35/schemas/imported.xsd")));
        assertFalse(guard.mayDecorate(URI.create("https://93.184.216.34/imported.xsd")));
    }

    @Test
    void testInternalImportIsRejectedBeforeAnyLookup() throws Exception {
        var resolver = resolver(false, null);
        assertThrows(ValidatorException.class, () -> resolver.resolveResource("http://www.w3.org/2001/XMLSchema", null, null, "http://169.254.169.254/latest/meta-data/", SCHEMA_SOURCE));
        verify(fileManager, never()).retrieveCachedRemoteResource(any(), any(), any(), anyBoolean());
        verify(fileManager, never()).getInputStreamFromURL(any(), any(), any(), any(), any(RemoteRequestGuard.class));
    }

    @Test
    void testCachedImportIsLoadedWithGuard() throws Exception {
        var cached = Files.writeString(root.resolve("cached.xsd"), "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
        when(fileManager.retrieveCachedRemoteResource(any(), any(), any(), anyBoolean())).thenReturn(cached);
        var input = resolver(false, null).resolveResource("http://www.w3.org/2001/XMLSchema", null, null, "https://93.184.216.34/imported.xsd", SCHEMA_SOURCE);
        assertNotNull(input);
        // Imports are by default considered to be those of a pre-configured schema.
        verify(fileManager).retrieveCachedRemoteResource(eq(domainConfig), eq(URI.create("https://93.184.216.34/imported.xsd")), any(RemoteRequestGuard.class), eq(false));
    }

    @Test
    void testCachedImportOfUserProvidedSchemaIsFlaggedAsSuch() throws Exception {
        var cached = Files.writeString(root.resolve("cached.xsd"), "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
        when(fileManager.retrieveCachedRemoteResource(any(), any(), any(), anyBoolean())).thenReturn(cached);
        var resolver = resolver(false, null);
        resolver.setUserProvidedSchema(true);
        resolver.resolveResource("http://www.w3.org/2001/XMLSchema", null, null, "https://93.184.216.34/imported.xsd", SCHEMA_SOURCE);
        verify(fileManager).retrieveCachedRemoteResource(eq(domainConfig), eq(URI.create("https://93.184.216.34/imported.xsd")), any(RemoteRequestGuard.class), eq(true));
    }

}
