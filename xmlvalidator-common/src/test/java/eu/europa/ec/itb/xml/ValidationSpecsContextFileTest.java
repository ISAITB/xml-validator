package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XSDFileResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the validation of user-provided context files against their configured XSD.
 */
class ValidationSpecsContextFileTest {

    @TempDir
    Path root;

    @Test
    void testContextFileThatIsInvalidAgainstItsSchemaIsReported() throws Exception {
        var schema = Files.writeString(root.resolve("context.xsd"), """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="a" type="xs:string"/>
                </xs:schema>
                """);
        var contextFile = Files.writeString(root.resolve("context.xml"), "<b/>");
        var input = Files.writeString(root.resolve("input.xml"), "<input/>");
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getTmpFolder()).thenReturn(root.resolve("tmp").toString());
        when(appConfig.getResourceRoot()).thenReturn(root.resolve("resources").toString());
        var domainConfig = mock(DomainConfig.class, Answers.RETURNS_DEEP_STUBS);
        when(domainConfig.getDomainRoot()).thenReturn(root.resolve("resources").resolve("domain").toString());
        try (var context = new AnnotationConfigApplicationContext()) {
            // Registered as existing singletons so that dependencies are not injected into the mocks.
            context.getBeanFactory().registerSingleton("fileManager", mock(FileManager.class));
            context.getBeanFactory().registerSingleton("applicationConfig", appConfig);
            context.register(XSDFileResolver.class);
            context.refresh();
            var contextFileConfig = new ContextFileConfig(Path.of("context.xml"), "context.xml", Optional.of(schema), false, false, Optional.empty(), 0, false);
            var specs = ValidationSpecs.builder(input.toFile(), new LocalisationHelper(Locale.ENGLISH), domainConfig, context)
                    .withValidationType("type")
                    .withContextFiles(List.of(new ContextFileData(contextFile, contextFileConfig)))
                    .build();
            // The failure to validate the context file is to be reported to the user, as opposed to an unexpected error.
            var error = assertThrows(ValidatorException.class, specs::getSchematronsToUse);
            assertTrue(error.isLocalised());
        }
    }

}
