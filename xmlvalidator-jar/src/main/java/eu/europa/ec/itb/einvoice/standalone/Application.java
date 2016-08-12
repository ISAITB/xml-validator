package eu.europa.ec.itb.einvoice.standalone;

import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by simatosc on 12/08/2016.
 */
@SpringBootApplication
@ComponentScan("eu.europa.ec.itb.einvoice")
public class Application {

    public static void main(String[] args) throws IOException {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        // Disabling System.err because Saxon by default writes errors to it.
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        prepareConfigForStandalone(ctx);
        ValidationRunner runner = ctx.getBean(ValidationRunner.class);
        runner.bootstrap(args);
    }

    private static void prepareConfigForStandalone(ApplicationContext ctx) throws IOException {
        ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
        config.setStandalone(true);
        // Explode validation resources to temp folder
        File tempFolder = Files.createTempDirectory("validation").toFile();
        File tempJarFile = new File(tempFolder, "validator-resources.jar");
        tempFolder.deleteOnExit();
        FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("validator-resources.jar"), tempJarFile);
        JarFile resourcesJar = new JarFile(tempJarFile);
        Enumeration entries = resourcesJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry)entries.nextElement();
            File f = new File(tempFolder, entry.getName());
            if (entry.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }
            FileUtils.copyInputStreamToFile(resourcesJar.getInputStream(entry), f);
        }
        // Set the resource root so that it can be used.
        String resourceRoot = tempFolder.getAbsolutePath();
        if (!resourceRoot.endsWith(File.separator)) {
            resourceRoot += File.separator;
        }
        config.setResourceRoot(resourceRoot);
    }

}
