package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the way in which {@link XSDFileResolver} instances (prototype beans) are obtained from the application context.
 */
class XSDFileResolverBeanCreationTest {

    private AnnotationConfigApplicationContext context() {
        var context = new AnnotationConfigApplicationContext();
        // Registered as existing singletons so that dependencies are not injected into the mocks.
        context.getBeanFactory().registerSingleton("fileManager", mock(FileManager.class));
        context.getBeanFactory().registerSingleton("applicationConfig", mock(ApplicationConfig.class));
        context.register(XSDFileResolver.class);
        context.refresh();
        return context;
    }

    @Test
    void testBeanCreationWithAllConstructorArguments() {
        try (var context = context()) {
            Consumer<HttpRequest.Builder> decorator = null;
            var resolver = context.getBean(XSDFileResolver.class, mock(DomainConfig.class), URI.create("file:///schema.xsd"), decorator, mock(ImportedUriAuthorizer.class), mock(ImportedFileAuthorizer.class));
            assertNotNull(resolver);
        }
    }

}
