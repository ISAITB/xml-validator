package eu.europa.ec.itb.xml.standalone;

import eu.europa.ec.itb.xml.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by simatosc on 12/08/2016.
 */
@SpringBootApplication
@ComponentScan("eu.europa.ec.itb")
public class Application {

    public static void main(String[] args) throws IOException {
        System.out.print("Starting validator ...");
        File tempFolder = Files.createTempDirectory("xmlvalidator").toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempFolder)));
        // Set the resource root so that it can be used. This is done before app startup to avoid PostConstruct issues.
        String resourceRoot = tempFolder.getAbsolutePath();
        if (!resourceRoot.endsWith(File.separator)) {
            resourceRoot += File.separator;
        }
        System.setProperty("validator.resourceRoot", resourceRoot);
        prepareConfigForStandalone(tempFolder);
        // Start the application.
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        // Post process config.
        ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
        config.setStandalone(true);
        // Set report folder for use as a temp file generation target.
        config.setTmpFolder(tempFolder.getAbsolutePath());
        // Disabling System.err because Saxon by default writes errors to it.
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        System.out.println(" Done.");
        try {
            ValidationRunner runner = ctx.getBean(ValidationRunner.class);
            runner.bootstrap(args, new File(config.getTmpFolder(), UUID.randomUUID().toString()));
        } catch (Exception e) {
            // Ignore errors here.
        }
    }

    private static void prepareConfigForStandalone(File tempFolder) throws IOException {
        // Explode invoice resources to temp folder
        File tempJarFile = new File(tempFolder, "validator-resources.jar");
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("validator-resources.jar")), tempJarFile);
        JarFile resourcesJar = new JarFile(tempJarFile);
        Enumeration<JarEntry> entries = resourcesJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File f = new File(tempFolder, entry.getName());
            if (entry.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }
            FileUtils.copyInputStreamToFile(resourcesJar.getInputStream(entry), f);
        }
    }

}
