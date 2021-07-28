package eu.europa.ec.itb.xml.standalone;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.jar.BaseValidationRunner;
import eu.europa.ec.itb.validation.commons.jar.FileReport;
import eu.europa.ec.itb.validation.commons.jar.ValidationInput;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Component that handles the actual triggering of validation and resulting reporting.
 */
@Component
@Scope("prototype")
public class ValidationRunner extends BaseValidationRunner<ValidationInput, DomainConfig> {

    private static final String FLAG__NO_REPORTS = "-noreports";
    private static final String FLAG__VALIDATION_TYPE = "-type";
    private static final String FLAG__INPUT = "-input";
    private static final String FLAG__XSD = "-xsd";
    private static final String FLAG__SCHEMATRON = "-sch";

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private FileManager fileManager;
    @Autowired
    private ReportGeneratorBean reportGenerator;

    /**
     * Get the XML content to validate based ont he provided path (can be a URL or file reference).
     *
     * @param contentPath The path to process.
     * @param parentFolder The validation run's temporary folder.
     * @return The file with the JSON content to use for the validation.
     * @throws IOException If an IO error occurs.
     */
    private File getContent(String contentPath, File parentFolder) throws IOException {
        File fileToUse;
        if (isValidURL(contentPath)) {
            // Value is a URL.
            try {
                fileToUse = fileManager.getFileFromURL(parentFolder, contentPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read file from URL ["+contentPath+"]");
            }
        } else {
            // Value is a local file. Copy this in the tmp folder as we may later be changing it (e.g. encoding updates).
            Path inputFile = Paths.get(contentPath);
            if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
                throw new IllegalArgumentException("Unable to read file ["+contentPath+"]");
            }
            Path finalInputFile = Paths.get(parentFolder.getAbsolutePath(), inputFile.getFileName().toString());
            Files.createDirectories(finalInputFile.getParent());
            fileToUse = Files.copy(inputFile, finalInputFile).toFile();
        }
        return fileToUse;
    }

    /**
     * Run the validation.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    @Override
    protected void bootstrapInternal(String[] args, File parentFolder) {
        // Process input arguments
        boolean typeRequired = domainConfig.hasMultipleValidationTypes();
        List<ValidationInput> inputs = new ArrayList<>();
        List<FileInfo> externalXsdInfo = new ArrayList<>();
        List<FileInfo> externalSchInfo = new ArrayList<>();
        boolean noReports = false;
        String validationType = null;
        try {
            int i = 0;
            while (i < args.length) {
                if (FLAG__NO_REPORTS.equalsIgnoreCase(args[i])) {
                    noReports = true;
                } else if (FLAG__VALIDATION_TYPE.equalsIgnoreCase(args[i])) {
                    validationType = argumentAsString(args, i);
                    if (validationType != null && !domainConfig.getType().contains(validationType)) {
                        throw new IllegalArgumentException("Unknown validation type. One of [" + String.join("|", domainConfig.getType()) + "] is needed.");
                    }
                } else if (FLAG__INPUT.equalsIgnoreCase(args[i])) {
                    if (args.length > i + 1) {
                        String path = args[++i];
                        inputs.add(new ValidationInput(getContent(path, parentFolder), path));
                    }
                } else if (FLAG__XSD.equalsIgnoreCase(args[i])) {
                    if (args.length > i + 1) {
                        externalXsdInfo = List.of(new FileInfo(getContent(args[++i], parentFolder)));
                    }
                } else if (FLAG__SCHEMATRON.equalsIgnoreCase(args[i])) {
                    if (args.length > i + 1) {
                        externalSchInfo.add(new FileInfo(getContent(args[++i], parentFolder)));
                    }
                }
                i++;
            }
            if (validationType == null) {
                if (typeRequired) {
                    throw new IllegalArgumentException("Validation type is required. One of [" + String.join("|", domainConfig.getType()) + "] needs tp be provided.");
                } else {
                    validationType = domainConfig.getType().get(0);
                }
            }
        } catch (IllegalArgumentException| ValidatorException e) {
            LOGGER_FEEDBACK.info("\nInvalid arguments provided: "+e.getMessage()+"\n");
            LOGGER.error("Invalid arguments provided: "+e.getMessage(), e);
            inputs.clear();
        } catch (Exception e) {
            LOGGER_FEEDBACK.info("\nAn error occurred while processing the provided arguments.\n");
            LOGGER.error("An error occurred while processing the provided arguments.", e);
            inputs.clear();
        }
        if (inputs.isEmpty()) {
            printUsage(typeRequired);
        } else {
            // Do validation
            StringBuilder summary = new StringBuilder();
            summary.append("\n");
            int i = 0;
            for (ValidationInput input: inputs) {
                LOGGER_FEEDBACK.info(String.format("\nValidating %s of %s ...", i + 1, inputs.size()));
                try {
                    XMLValidator validator = ctx.getBean(XMLValidator.class, input.getInputFile(), validationType, externalXsdInfo, externalSchInfo, domainConfig);
                    TAR report = validator.validateAll();
                    if (report == null) {
                        summary.append("\nNo validation report was produced.\n");
                    } else {
                        int itemCount = 0;
                        if (report.getReports() != null && report.getReports().getInfoOrWarningOrError() != null) {
                            itemCount = report.getReports().getInfoOrWarningOrError().size();
                        }
                        if (!noReports) {
                            File xmlReportFile = new File(Path.of(System.getProperty("user.dir")).toFile(), "report."+i+".xml");
                            Files.deleteIfExists(xmlReportFile.toPath());
                            // Create XML report
                            fileManager.saveReport(report, xmlReportFile, domainConfig);
                            FileReport reportData = new FileReport(input.getFileName(), report, typeRequired, validationType);
                            summary.append("\n").append(reportData.toString()).append("\n");
                            if (itemCount <= domainConfig.getMaximumReportsForDetailedOutput()) {
                                // Create PDF report
                                File pdfReportFile = new File(xmlReportFile.getParentFile(), "report."+i+".pdf");
                                Files.deleteIfExists(pdfReportFile.toPath());
                                reportGenerator.writeReport(domainConfig, xmlReportFile, pdfReportFile);
                                summary.append("- Detailed reports in [").append(xmlReportFile.getAbsolutePath()).append("] and [").append(pdfReportFile.getAbsolutePath()).append("] \n");
                            } else if (report.getCounters() != null && (report.getCounters().getNrOfAssertions().longValue() + report.getCounters().getNrOfErrors().longValue() + report.getCounters().getNrOfWarnings().longValue()) <= domainConfig.getMaximumReportsForXmlOutput()) {
                                summary.append("- Detailed report in [").append(xmlReportFile.getAbsolutePath()).append("] (PDF report skipped due to large number of report items) \n");
                            } else {
                                summary.append("- Detailed report in [").append(xmlReportFile.getAbsolutePath()).append("] (report limited to first ").append(domainConfig.getMaximumReportsForXmlOutput()).append(" items and PDF report skipped) \n");
                            }
                        }
                    }
                } catch (ValidatorException e) {
                    LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation: "+e.getMessage());
                    LOGGER.error("An error occurred while executing the validation: "+e.getMessage(), e);
                    break;

                } catch (Exception e) {
                    LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation.");
                    LOGGER.error("An error occurred while executing the validation: "+e.getMessage(), e);
                    break;
                }
                i++;
                LOGGER_FEEDBACK.info(" Done.");
            }
            LOGGER_FEEDBACK.info(summary.toString());
            LOGGER_FEEDBACK_FILE.info(summary.toString());
        }
    }

    /**
     * Print the usage string for the validator.
     *
     * @param requireType True if the validation type should be included in the message.
     */
    private void printUsage(boolean requireType) {
        StringBuilder usageMessage = new StringBuilder();
        StringBuilder parametersMessage = new StringBuilder();
        usageMessage.append("\nExpected usage: java -jar validator.jar ").append(FLAG__INPUT).append(" FILE_OR_URI_1 ... [").append(FLAG__INPUT).append(" FILE_OR_URI_N] [").append(FLAG__NO_REPORTS).append("]");
        if (requireType) {
            usageMessage.append(" [").append(FLAG__VALIDATION_TYPE).append(" VALIDATION_TYPE]");
            parametersMessage.append("\n").append(PAD).append(PAD).append("- VALIDATION_TYPE is the type of validation to perform, one of [").append(String.join("|", domainConfig.getType())).append("].");
        }
        if (domainConfig.definesTypeWithExternalXsd()) {
            usageMessage.append(" [").append(FLAG__XSD).append(" SCHEMA_FILE_OR_URI]");
            parametersMessage.append("\n").append(PAD).append(PAD).append("- SCHEMA_FILE_OR_URI is the full file path or URI to a schema for the validation.");
        }
        if (domainConfig.definesTypeWithExternalSchematrons()) {
            usageMessage.append(" [").append(FLAG__SCHEMATRON).append(" SCHEMATRON_FILE_OR_URI_1] ... [").append(FLAG__SCHEMATRON).append(" SCHEMATRON_FILE_OR_URI_N]");
            parametersMessage.append("\n").append(PAD).append(PAD).append("- SCHEMATRON_FILE_OR_URI_X is the full file path or URI to a schematron file for the validation.");
        }
        usageMessage.append("\n").append(PAD).append("Where:");
        usageMessage.append("\n").append(PAD).append(PAD).append("- FILE_OR_URI_X is the full file path or URI to the content to validate.");
        usageMessage.append(parametersMessage);
        usageMessage.append("\n\nThe summary of each validation will be printed and the detailed reports produced in the current directory (as \"report.X.xml\" and \"report.X.pdf\").");
        System.out.println(usageMessage);
    }

}
