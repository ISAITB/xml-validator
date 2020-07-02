package eu.europa.ec.itb.xml.config;

import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public DomainPluginConfigProvider pluginConfigProvider() {
        return new DomainPluginConfigProvider();
    }

}
