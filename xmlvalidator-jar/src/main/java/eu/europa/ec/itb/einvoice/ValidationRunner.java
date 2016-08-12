package eu.europa.ec.itb.einvoice;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.validation.FileReport;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by simatosc on 12/08/2016.
 */
@SpringBootApplication
@Component
public class ValidationRunner implements ApplicationContextAware {

    private static Logger logger = LoggerFactory.getLogger(ValidationRunner.class);

    @Autowired
    ApplicationConfig config;
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(ValidationRunner.class, args);
        ValidationRunner runner = ctx.getBean(ValidationRunner.class);
        runner.bootstrap(args);
    }

    private void bootstrap(String[] args) {
        System.out.println("LALALA");
        // Process input arguments
        List<ValidationInput> inputs = new ArrayList<>();
        boolean noReports = false;
        boolean requireType = config.hasMultipleValidationTypes();
        try {
            int i = 0;
            while (i < args.length) {
                if ("-noreports".equalsIgnoreCase(args[i])) {
                    noReports = true;
                } else if ("-file".equalsIgnoreCase(args[i])) {
                    String type = null;
                    String filePath = null;
                    if (requireType) {
                        // The next two arguments are the type and file path
                        if (args.length > i+2) {
                            type = args[++i];
                            filePath = args[++i];
                        }
                    } else {
                        type = config.getType().get(0);
                        // The next argument is the file path
                        if (args.length > i+1) {
                            filePath = args[++i];
                        }
                    }
                    if (!config.getType().contains(type)) {
                        throw new IllegalArgumentException("Unknown validation type ["+type+"]");
                    }
                    File inputFile = new File(filePath);
                    if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                        throw new IllegalArgumentException("Unable to read file ["+filePath+"]");
                    }
                    inputs.add(new ValidationInput(inputFile, type));
                } else {
                    throw new IllegalArgumentException("Unexpected parameter ["+args[i]+"]");
                }
                i++;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\nInvalid arguments provided: "+e.getMessage());
        }
        if (inputs.isEmpty()) {
            printUsage();
        } else {
            // Proceed with validation.
            StringBuilder summary = new StringBuilder();
            summary.append("\n");
            for (ValidationInput input: inputs) {
                try (FileInputStream stream = new FileInputStream(input.getInputFile())) {
                    XMLValidator validator = applicationContext.getBean(XMLValidator.class, stream, input.getValidationType());
                    TAR report = validator.validateAll();
                    // TODO save report
                    FileReport reporter = new FileReport(input.getInputFile().getAbsolutePath(), report);
                    summary.append(reporter.toString()).append("\n");
                } catch (Exception e) {
                    System.out.println("An unexpected error occurred: "+e.getMessage());
                    // TODO log exception.
                }
            }
            logger.info(summary.toString());
            System.out.println(summary.toString());
        }
    }

    private void printUsage() {
        boolean requireType = config.hasMultipleValidationTypes();
        StringBuilder msg = new StringBuilder();
        if (requireType) {
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] -file TYPE_1 FILE_1 [-file TYPE_2 FILE_2] ... [-file TYPE_N FILE_N]");
            msg.append("\n\tWhere TYPE_X is the type of validation to perform for the file (accepted values: ");
            for (int i=0; i < config.getType().size(); i++) {
                String type = config.getType().get(i);
                msg.append(type);
                if (i+1 < config.getType().size()) {
                    msg.append(", ");
                }
            }
            msg.append(")");
        } else {
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] -file FILE_1 [-file FILE_2] ... [-file FILE_N]");
        }
        msg.append("\n\tWhere FILE_X is the full path to a file to validate");
        msg.append("\n\nThe validation summary of each file will be printed and the detailed validation report will produced at the location of the input file (with a \".report.xml\" postfix). Providing \"-noreports\" as the first flag skips the detailed report generation.");
        System.out.println(msg.toString());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
