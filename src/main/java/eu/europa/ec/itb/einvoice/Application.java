package eu.europa.ec.itb.einvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * Created by simatosc on 25/02/2016.
 */
@SpringBootApplication
@EnableScheduling
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
   }

}
