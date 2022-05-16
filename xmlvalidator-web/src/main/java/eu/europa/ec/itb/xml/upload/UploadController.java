package eu.europa.ec.itb.xml.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportItemComparator;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.BaseUploadController;
import eu.europa.ec.itb.validation.commons.web.Constants;
import eu.europa.ec.itb.validation.commons.web.dto.Translations;
import eu.europa.ec.itb.validation.commons.web.dto.UploadResult;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.itb.validation.commons.web.Constants.*;

/**
 * Controller to manage the validator's web user interface.
 */
@Controller
public class UploadController extends BaseUploadController<DomainConfig, DomainConfigCache> {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    public static final String CONTENT_TYPE_FILE = "fileType" ;
    public static final String CONTENT_TYPE_URI = "uriType" ;
    public static final String CONTENT_TYPE_STRING = "stringType" ;

    @Autowired
    FileManager fileManager;
    @Autowired
    BeanFactory beans;
    @Autowired
    ApplicationConfig appConfig;
    @Autowired
    InputHelper inputHelper;
    @Autowired
    private CustomLocaleResolver localeResolver;

    /**
     * Prepare the upload page.
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        var config = getDomainConfig(request);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_DOMAIN_CONFIG, config);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        attributes.put(PARAM_MINIMAL_UI, request.getAttribute(IS_MINIMAL));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, config.getExternalArtifactInfoMap());
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));
        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Prepare the upload page (minimal UI version).
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/uploadm")
    public ModelAndView uploadMinimal(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        return upload(domain, request, response);
    }

    /**
     * Handle the upload form's submission.
     *
     * @param domain The domain name.
     * @param file The input file (if provided via file upload).
     * @param uri The input URI (if provided via remote URI).
     * @param string The input content (if provided via editor).
     * @param validationType The validation type.
     * @param contentType The type of the provided content.
     * @param externalSchemaContentType The content type of the user-provided schemas.
     * @param externalSchemaFiles The user-provided schemas (those provided as file uploads).
     * @param externalSchemaUri The user-provided schemas (those provided as URIs).
     * @param externalSchContentType The content type of the user-provided Schematron files.
     * @param externalSchFiles The user-provided Schematron files (those provided as file uploads).
     * @param externalSchUri The user-provided Schematron files (those provided as URIs).
     * @param redirectAttributes Redirect attributes.
     * @param request The received request.
     * @param response The HTTP response.
     * @return The model and view information.
     */
    @PostMapping(value = "/{domain}/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult<Translations> handleUpload(@PathVariable("domain") String domain,
                                     @RequestParam(value = "file", required = false) MultipartFile file,
                                     @RequestParam(value = "uri", defaultValue = "") String uri,
                                     @RequestParam(value = "text-editor", defaultValue = "") String string,
                                     @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                     @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                     @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaContentType,
                                     @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
                                     @RequestParam(value = "uri-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
                                     @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchContentType,
                                     @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
                                     @RequestParam(value = "uri-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        var config = getDomainConfig(request);
        var localisationHelper = new LocalisationHelper(config, localeResolver.resolveLocale(request, response, config, appConfig));
        var result = new UploadResult<>();

        if (StringUtils.isBlank(validationType)) {
            validationType = config.getType().get(0);
        }
        if (config.hasMultipleValidationTypes() && (validationType == null || !config.getType().contains(validationType))) {
            // A validation type is required.
            result.setMessage(localisationHelper.localise("validator.label.exception.providedValidationTypeNotValid"));
        } else {
            File tempFolderForRequest = fileManager.createTemporaryFolderPath();
            var proceedToValidate = true;
            try {
                File inputFile = null;
                try {
                    inputFile = saveInput(contentType, file, uri, string, tempFolderForRequest);
                    if (inputFile == null || !fileManager.checkFileType(inputFile)) {
                        proceedToValidate = false;
                        result.setMessage(localisationHelper.localise("validator.label.exception.providedInputNotXML"));
                    }
                } catch (IOException e) {
                    logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                    result.setMessage(localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
                    proceedToValidate = false;
                }
                if (proceedToValidate) {
                    List<FileInfo> externalSchIS = new ArrayList<>();
                    List<FileInfo> externalSchemaIS = new ArrayList<>();
                    try {
                        externalSchemaIS = getExternalFiles(config, externalSchemaContentType, externalSchemaFiles, externalSchemaUri, config.getSchemaInfo(validationType), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA, tempFolderForRequest);
                        externalSchIS = getExternalFiles(config, externalSchContentType, externalSchFiles, externalSchUri, config.getSchematronInfo(validationType), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON, tempFolderForRequest);
                    } catch (Exception e) {
                        logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
                        result.setMessage(localisationHelper.localise("validator.label.exception.errorInUpload", e.getMessage()));
                        proceedToValidate = false;
                    }
                    if (proceedToValidate) {
                        XMLValidator validator = beans.getBean(XMLValidator.class, inputFile, validationType, externalSchemaIS, externalSchIS, config, localisationHelper);
                        TAR report = validator.validateAll();
                        TAR aggregateReport = Utils.toAggregatedTAR(report, localisationHelper);
                        if (config.isReportsOrdered() && aggregateReport.getReports() != null) {
                            aggregateReport.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
                        }
                        // Cache detailed report.
                        try {
                            String inputToInclude = null;
                            if (report.getContext() != null && !report.getContext().getItem().isEmpty()) {
                                inputToInclude = report.getContext().getItem().get(0).getValue();
                            }
                            String inputID = fileManager.writeXML(config.getDomainName(), inputToInclude);
                            fileManager.saveReport(report, inputID, config);
                            fileManager.saveReport(aggregateReport, inputID, config, true);
                            String fileName;
                            if (contentType.equals(CONTENT_TYPE_FILE)) {
                                fileName = file.getOriginalFilename();
                            } else if (contentType.equals(CONTENT_TYPE_URI)) {
                                fileName = uri;
                            } else {
                                fileName = "-";
                            }
                            result.populateCommon(localisationHelper, validationType, config, inputID,
                                    fileName, report, aggregateReport,
                                    new Translations(localisationHelper, report, config));
                        } catch (IOException e) {
                            logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                            result.setMessage(localisationHelper.localise("validator.label.exception.errorGeneratingDetailedReport", e.getMessage()));
                        }
                    }
                }
            } catch (ValidatorException e) {
                logger.error(e.getMessageForLog(), e);
                result.setMessage(e.getMessageForDisplay(localisationHelper));
            } catch (Exception e) {
                logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
                if (e.getMessage() != null) {
                    result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidationWithParams", e.getMessage()));
                } else {
                    result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidation"));
                }
            } finally {
                // Cleanup temporary resources for request.
                if (tempFolderForRequest.exists()) {
                    FileUtils.deleteQuietly(tempFolderForRequest);
                }
            }
        }
        return result;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String[], MultipartFile[], String[], RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult<Translations> handleUploadMinimal(@PathVariable("domain") String domain,
                                      @RequestParam(value = "file", required = false) MultipartFile file,
                                      @RequestParam(value = "uri", defaultValue = "") String uri,
                                      @RequestParam(value = "text-editor", defaultValue = "") String string,
                                      @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                      @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                      @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchema,
                                      @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
                                      @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
                                      @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSch,
                                      @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
                                      @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        return handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, externalSch, externalSchFiles, externalSchUri, redirectAttributes, request, response);
    }

    /**
     * Handle the upload form's submission when the user interface is embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String[], MultipartFile[], String[], RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/upload", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadEmbedded(@PathVariable("domain") String domain,
                                             @RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "uri", defaultValue = "") String uri,
                                             @RequestParam(value = "text-editor", defaultValue = "") String string,
                                             @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                             @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                             @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchema,
                                             @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
                                             @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
                                             @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSch,
                                             @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
                                             @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        var uploadForm = upload(domain, request, response);
        var uploadResult = handleUpload(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, externalSch, externalSchFiles, externalSchUri, redirectAttributes, request, response);
        uploadForm.getModel().put(Constants.PARAM_REPORT_DATA, writeResultToString(uploadResult));
        return uploadForm;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal and embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String[], MultipartFile[], String[], String[], MultipartFile[], String[], RedirectAttributes, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadMinimalEmbedded(@PathVariable("domain") String domain,
                                             @RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "uri", defaultValue = "") String uri,
                                             @RequestParam(value = "text-editor", defaultValue = "") String string,
                                             @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                             @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                             @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchema,
                                             @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required= false) MultipartFile[] externalSchemaFiles,
                                             @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMA, required = false) String[] externalSchemaUri,
                                             @RequestParam(value = "contentType-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSch,
                                             @RequestParam(value = "inputFile-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required= false) MultipartFile[] externalSchFiles,
                                             @RequestParam(value = "uriToValidate-external_"+DomainConfig.ARTIFACT_TYPE_SCHEMATRON, required = false) String[] externalSchUri,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        return handleUploadEmbedded(domain, file, uri, string, validationType, contentType, externalSchema, externalSchemaFiles, externalSchemaUri, externalSch, externalSchFiles, externalSchUri, redirectAttributes, request, response);
    }

    /**
     * Validate and get the user-provided XSDs or Schematron files.
     *
     * @param domainConfig The domain configuration.
     * @param externalContentType The directly provided schemas.
     * @param externalFiles The schemas provided as files.
     * @param externalUri The schemas provided as URIs.
     * @param artifactInfo The schema information from the domain.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @param parentFolder The temporary folder to use for file system storage.
     * @return The list of user-provided artifacts.
     * @throws IOException If an error occurs.
     */
    private List<FileInfo> getExternalFiles(DomainConfig domainConfig, String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri,
                                            ValidationArtifactInfo artifactInfo, String validationType,
                                            String artifactType, File parentFolder) throws IOException {
        List<FileInfo> artifacts = new ArrayList<>();
        if (externalContentType != null) {
            for (int i=0; i<externalContentType.length; i++) {
                File file = null;
                if (CONTENT_TYPE_FILE.equals(externalContentType[i])) {
                    if (!externalFiles[i].isEmpty()) {
                        file = fileManager.getFileFromInputStream(parentFolder, externalFiles[i].getInputStream(), null, externalFiles[i].getOriginalFilename());
                    }
                } else {
                    if (StringUtils.isNotBlank(externalUri[i])) {
                        file = fileManager.getFileFromURL(parentFolder, externalUri[i], null, null, artifactType);
                    }
                }
                if (file != null) {
                    File rootFile = this.fileManager.unzipFile(parentFolder, file);
                    if (rootFile == null) {
                        artifacts.add(new FileInfo(fileManager.preprocessFileIfNeeded(domainConfig, validationType, artifactType, file, true)));
                    } else {
                        // ZIP File
                        boolean proceed;
                        if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
                            proceed = inputHelper.validateSchemaZip(rootFile);
                        } else {
                            proceed = true;
                        }
                        if (proceed) {
                            artifacts.addAll(fileManager.getLocalValidationArtifacts(rootFile, DomainConfig.ARTIFACT_TYPE_SCHEMA));
                        } else {
                            var exception = new ValidatorException("validator.label.exception.errorDuringExternalXSDValidation");
                            logger.error(exception.getMessageForLog());
                            throw exception;
                        }
                    }
                }
            }
        }
        if (validateExternalFiles(artifacts, artifactInfo) && (!DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType) || artifacts.size() <= 1)) {
            return artifacts;
        } else {
            var exception = new ValidatorException("validator.label.exception.errorDuringValidationExternalSchema");
            logger.error(exception.getMessageForLog());
            throw exception;
        }
    }

    /**
     * Validate the list of user-provided schemas.
     *
     * @param lis The schemas.
     * @param artifactInfo The schema information from the domain configuration.
     * @return True for correctly provided schemas.
     */
    private boolean validateExternalFiles(List<FileInfo> lis, ValidationArtifactInfo artifactInfo) {
        ExternalArtifactSupport externalArtifactSupport = artifactInfo.getExternalArtifactSupport();

        boolean validated = false;

        switch(externalArtifactSupport) {
            case REQUIRED:
                if(lis!=null && !lis.isEmpty()) {
                    validated = true;
                }
                break;
            case OPTIONAL:
                validated = true;
                break;
            case NONE:
                if(lis==null || lis.isEmpty()) {
                    validated = true;
                }
                break;
        }

        return validated;
    }

    /**
     * Save the input XML to validate in the validator's temp file system folder.
     *
     * @param inputType The way in which to load the input.
     * @param file The input as a file.
     * @param uri The input as a URI.
     * @param string The input as directly provided text
     * @param parentFolder The temp folder to store the file in.
     * @return The stored file.
     * @throws IOException If an IO error occurs.
     */
    private File saveInput(String inputType, MultipartFile file, String uri, String string, File parentFolder) throws IOException {
        File inputFile;
        switch (inputType) {
            case CONTENT_TYPE_FILE:
                inputFile = fileManager.getFileFromInputStream(parentFolder, file.getInputStream(), null, null);
                break;
            case CONTENT_TYPE_URI:
                inputFile = fileManager.getFileFromURL(parentFolder, uri);
                break;
            case CONTENT_TYPE_STRING:
                inputFile = fileManager.getFileFromString(parentFolder, string);
                break;
            default: throw new IllegalArgumentException("Unsupported input content type ["+inputType+"]");
        }
        return inputFile;
    }

}
