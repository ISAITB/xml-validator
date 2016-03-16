package eu.europa.ec.itb.einvoice.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Created by simatosc on 16/03/2016.
 */
@Configuration
public class MailConfig {

    @Autowired
    eu.europa.ec.itb.einvoice.Configuration config;

    @Bean
    public JavaMailSender javaMailService() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(config.getMailOutboundHost());
        impl.setPort(config.getMailOutboundPort());
        Properties props = new Properties();
        if (config.isMailAuthEnable()) {
            props.setProperty("mail.smtp.auth", "true");
            impl.setUsername(config.getMailAuthUsername());
            impl.setPassword(config.getMailAuthPassword());
        }
        if (config.isMailOutboundSSLEnable()) {
            props.setProperty("mail.smtp.ssl.enable", "true");
        }
        impl.setJavaMailProperties(props);
        return impl;
    }

}
