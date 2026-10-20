package eu.europa.ec.itb.xml.validation;

import com.helger.io.resource.FileSystemResource;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.xslt.SchematronResourceXSLT;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.ResourceCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the restriction of the collections that can be looked up during Schematron processing.
 */
class SchematronCollectionAuthorizerTest {

    @TempDir
    Path root;
    private Path tmpFolder;
    private Path outsideFolder;
    private ImportedUriAuthorizer uriAuthorizer;
    private ImportedFileAuthorizer fileAuthorizer;

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
    }

    private void assertAllowed(String collectionUri) throws Exception {
        var delegate = mock(CollectionFinder.class);
        var expected = mock(ResourceCollection.class);
        when(delegate.findCollection(any(), eq(collectionUri))).thenReturn(expected);
        var finder = new SchematronCollectionAuthorizer(delegate, uriAuthorizer, fileAuthorizer);
        assertSame(expected, finder.findCollection(null, collectionUri));
    }

    private void assertBlocked(String collectionUri) throws Exception {
        var delegate = mock(CollectionFinder.class);
        var finder = new SchematronCollectionAuthorizer(delegate, uriAuthorizer, fileAuthorizer);
        assertThrows(ValidatorException.class, () -> finder.findCollection(null, collectionUri));
        verify(delegate, never()).findCollection(any(), any());
    }

    @Test
    void testLocationWithinAllowedFolderIsAllowed() throws Exception {
        assertAllowed(tmpFolder.toUri().toString());
        assertAllowed(tmpFolder.resolve("request").toUri() + "?select=*.xml;recurse=yes");
    }

    @Test
    void testLocationOutsideAllowedFoldersIsBlocked() throws Exception {
        assertBlocked(outsideFolder.toUri().toString());
        assertBlocked(outsideFolder.toUri() + "?select=*.xml");
        assertBlocked(outsideFolder.toUri() + "?select=*;unparsed=yes");
    }

    @Test
    void testTraversalOutOfAllowedFolderIsBlocked() throws Exception {
        assertBlocked(tmpFolder.toUri() + "../outside/");
    }

    @Test
    void testInternalAndOtherLocationsAreBlocked() throws Exception {
        assertBlocked("http://127.0.0.1:1/collection");
        assertBlocked("http://169.254.169.254/latest/");
        assertBlocked("jar:" + tmpFolder.toUri() + "x.jar!/");
        assertBlocked("relative/folder/");
        assertBlocked("ftp://example.org/folder/");
    }

    @Test
    void testDefaultCollectionIsDelegated() throws Exception {
        var delegate = mock(CollectionFinder.class);
        var expected = mock(ResourceCollection.class);
        when(delegate.findCollection(any(), eq(null))).thenReturn(expected);
        assertSame(expected, new SchematronCollectionAuthorizer(delegate, uriAuthorizer, fileAuthorizer).findCollection(null, null));
    }

    @Test
    void testTrustedProcessingWithoutAuthorizersIsUnchanged() throws Exception {
        var delegate = mock(CollectionFinder.class);
        var expected = mock(ResourceCollection.class);
        var location = outsideFolder.toUri().toString();
        when(delegate.findCollection(any(), eq(location))).thenReturn(expected);
        assertSame(expected, new SchematronCollectionAuthorizer(delegate, null, null).findCollection(null, location));
    }

    /**
     * Run an XSLT-based Schematron (set up in the same way as in the validator) that reports what the collection at
     * the given location contains.
     */
    private String runSchematronReadingCollection(String collectionUri) throws Exception {
        var xslt = tmpFolder.resolve("schematron.xsl");
        Files.writeString(xslt, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:svrl="http://purl.oclc.org/dsdl/svrl">
                  <xsl:template match="/">
                    <svrl:schematron-output title="test">
                      <svrl:failed-assert test="x" location="/">
                        <svrl:text>FOUND:<xsl:value-of select="string-join(collection('%s')//value, ',')"/></svrl:text>
                      </svrl:failed-assert>
                    </svrl:schematron-output>
                  </xsl:template>
                </xsl:stylesheet>
                """.formatted(collectionUri));
        var schematron = SchematronResourceXSLT.builder(new FileSystemResource(xslt.toFile()))
                .validateSVRL(false)
                .useCache(false)
                .transformerFactoryCustomizer(factory -> SchematronCollectionAuthorizer.install(factory, uriAuthorizer, fileAuthorizer))
                .build();
        var output = schematron.applySchematronValidationToSVRL(new StreamSource(new StringReader("<root/>")));
        assertNotNull(output);
        return SVRLHelper.getAllFailedAssertions(output).get(0).getText();
    }

    @Test
    void testSchematronCanReadCollectionWithinAllowedFolder() throws Exception {
        var data = Files.createDirectories(tmpFolder.resolve("request"));
        Files.writeString(data.resolve("data.xml"), "<doc><value>inside</value></doc>");
        assertEquals("FOUND:inside", runSchematronReadingCollection(data.toUri() + "?select=data.xml").trim());
    }

    @Test
    void testSchematronCannotReadCollectionOutsideAllowedFolders() throws Exception {
        Files.writeString(outsideFolder.resolve("data.xml"), "<doc><value>outside</value></doc>");
        var location = outsideFolder.toUri() + "?select=data.xml";
        assertThrows(Exception.class, () -> runSchematronReadingCollection(location));
    }

}
