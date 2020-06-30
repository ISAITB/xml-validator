package eu.europa.ec.itb.xml.ws;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Created by simatosc on 26/02/2016.
 */
@Configuration
public class ValidationServiceConfig {

    public static final String CXF_ROOT = "api";

    @Autowired
    Bus cxfBus;

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private DomainConfigCache domainConfigCache;

    @Autowired
    private ApplicationContext applicationContext;


    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        ServletRegistrationBean<CXFServlet> srb = new ServletRegistrationBean<>(new CXFServlet(), "/"+ CXF_ROOT +"/*");
        srb.addInitParameter("hide-service-list-page", "true");
        return srb;
    }

    /**
     * The CXF endpoint(s) that will serve service calls.
     */
    @PostConstruct
    public void publishValidationServices() {
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.SOAP_API)) {
                EndpointImpl endpoint = new EndpointImpl(cxfBus, applicationContext.getBean(ValidationServiceImpl.class, domainConfig));
                endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
                endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
                endpoint.publish("/"+domainConfig.getDomainName()+"/validation");
            }
        }
    }

}
