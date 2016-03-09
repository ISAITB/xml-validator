package eu.europa.ec.itb.einvoice.ws;

import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.validation.XmlValidator;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.xml.xsd.XsdSchemaCollection;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;

/**
 * Created by simatosc on 29/02/2016.
 */
@EnableWs
@Configuration
public class WebServiceConfig2 {

    @Bean
    public ServletRegistrationBean messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean(servlet, "/api2/*");
    }

    @Bean(name = "gitb")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchemaCollection gitbSchemas) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("ValidationService");
        wsdl11Definition.setLocationUri("/api2");
        wsdl11Definition.setTargetNamespace(ValidationService2.NAMESPACE_URI);
        wsdl11Definition.setSchemaCollection(gitbSchemas);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchemaCollection gitbSchemas() {
        CommonsXsdSchemaCollection collection = new CommonsXsdSchemaCollection();
        collection.setXsds(
                new ClassPathResource("wsdl/gitb_core.xsd"),
                new ClassPathResource("wsdl/gitb_tr.xsd"),
                new ClassPathResource("wsdl/gitb_vs.xsd")
        );
        collection.setInline(true);
        return collection;
    }

}
