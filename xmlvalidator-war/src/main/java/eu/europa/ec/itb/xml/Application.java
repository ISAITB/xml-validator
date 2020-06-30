package eu.europa.ec.itb.xml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by simatosc on 25/02/2016.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan("eu.europa.ec.itb")
public class Application  {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
