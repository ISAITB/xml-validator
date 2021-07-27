package eu.europa.ec.itb.xml.config;

import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure common spring beans.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Support the definition of plugins.
     *
     * @return The default plugin provider.
     */
    @Bean
    public DomainPluginConfigProvider pluginConfigProvider() {
        return new DomainPluginConfigProvider();
    }

}
