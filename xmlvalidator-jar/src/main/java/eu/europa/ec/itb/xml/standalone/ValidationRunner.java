package eu.europa.ec.itb.xml.standalone;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.FileReport;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by simatosc on 12/08/2016.
 */
@Component
@Scope("prototype")
public class ValidationRunner {

    private static Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
    private static Logger loggerFeedback = LoggerFactory.getLogger("FEEDBACK");
    private static Logger loggerFeedbackFile = LoggerFactory.getLogger("VALIDATION_RESULT");

    private DomainConfig domainConfig;

    @Autowired
    FileManager fileManager;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    DomainConfigCache domainConfigCache;

    @PostConstruct
    public void init() {
        // Determine the domain configuration.
        List<DomainConfig> domainConfigurations = domainConfigCache.getAllDomainConfigurations();
        if (domainConfigurations.size() == 1) {
            this.domainConfig = domainConfigurations.get(0);
        } else if (domainConfigurations.size() > 1) {
            throw new IllegalArgumentException("A specific validation domain needs to be specified. Do this by supplying property [validator.domain].");
        } else {
            throw new IllegalStateException("No validation domains could be found.");
        }
    }

    protected void bootstrap(String[] args) {
        // Process input arguments
        List<ValidationInput> inputs = new ArrayList<>();
        boolean noReports = false;
        boolean requireType = domainConfig.hasMultipleValidationTypes();
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
                        type = domainConfig.getType().get(0);
                        // The next argument is the file path
                        if (args.length > i+1) {
                            filePath = args[++i];
                        }
                    }
                    if (!domainConfig.getType().contains(type)) {
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
            loggerFeedback.info("\nInvalid arguments provided: "+e.getMessage());
            inputs.clear();
        }
        if (inputs.isEmpty()) {
            printUsage();
        } else {
            // Proceed with validation.
            StringBuilder summary = new StringBuilder();
            summary.append("\n");
            for (ValidationInput input: inputs) {
                loggerFeedback.info("\nValidating ["+input.getInputFile().getAbsolutePath()+"]...");
                try {
                    XMLValidator validator = applicationContext.getBean(XMLValidator.class, input.getInputFile(), input.getValidationType(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, domainConfig);
                    TAR report = validator.validateAll();
                    FileReport reporter = new FileReport(input.getInputFile().getAbsolutePath(), report, !noReports, false);
                    if (!noReports) {
                        // Serialize report.
                        fileManager.saveReport(report, new File(reporter.getReportXmlFileName()));
                    }
                    summary.append("\n").append(reporter.toString()).append("\n");
                } catch (Exception e) {
                    loggerFeedback.info("\nAn unexpected error occurred: "+e.getMessage());
                    logger.error("An unexpected error occurred: "+e.getMessage(), e);
                    break;
                }
                loggerFeedback.info(" Done.\n");
            }
            loggerFeedback.info(summary.toString());
            loggerFeedbackFile.info(summary.toString());
        }
    }

    private void printUsage() {
        boolean requireType = domainConfig.hasMultipleValidationTypes();
        StringBuilder msg = new StringBuilder();
        if (requireType) {
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] -file TYPE_1 FILE_1 [-file TYPE_2 FILE_2] ... [-file TYPE_N FILE_N]");
            msg.append("\n\tWhere TYPE_X is the type of validation to perform for the file (accepted values: ");
            for (int i=0; i < domainConfig.getType().size(); i++) {
                String type = domainConfig.getType().get(i);
                msg.append(type);
                if (i+1 < domainConfig.getType().size()) {
                    msg.append(", ");
                }
            }
            msg.append(")");
        } else {
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] -file FILE_1 [-file FILE_2] ... [-file FILE_N]");
        }
        msg.append("\n\tWhere FILE_X is the full path to a file to validate");
        msg.append("\n\nThe validation summary of each file will be printed and the detailed validation report will be produced at the location of the input file (with a \".report.xml\" postfix). Providing \"-noreports\" as the first flag skips the detailed report generation.");
        System.out.println(msg.toString());
    }

}
