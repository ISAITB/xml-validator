package eu.europa.ec.itb.xml.rest;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.BaseRestController;
import eu.europa.ec.itb.validation.commons.web.rest.model.ApiInfo;
import eu.europa.ec.itb.validation.commons.web.rest.model.Output;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.rest.model.Input;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

/**
 * REST controller to allow triggering the validator via its REST API.
 */
@Tag(name = "/rest/{domain}/api", description = "Operations for the validation of XML content against XML Schemas and Schematrons.")
@RestController
public class RestValidationController extends BaseRestController<DomainConfig, ApplicationConfig, FileManager, InputHelper> {

    @Autowired
    private ApplicationContext ctx = null;
    @Autowired
    private FileManager fileManager = null;

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(value = "/rest/{domain}/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo info(@PathVariable("domain") String domain) {
        return super.info(domain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping(value = "/rest/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo[] infoAll() {
        return super.infoAll();
    }

    /**
     * Service to trigger one validation for the provided input and settings.
     *
     * @param domain The relevant domain for the validation.
     * @param in The input for the validation.
     * @param request The HTTP request.
     * @return The result of the validator.
     */
    @Operation(summary = "Validate a single XML document.", description="Validate a single XML document. The content can be provided either within the request as a BASE64 encoded string or remotely as a URL.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE))
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(value = "/rest/{domain}/api/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<StreamingResponseBody> validate(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one XML document).")
            @RequestBody Input in,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        /*
         * Important: We call executeValidationProcess here and not in the return statement because the StreamingResponseBody
         * uses a separate thread. Doing so would break the ThreadLocal used in the statistics reporting.
         */
        var report = executeValidationProcess(in, domainConfig);
        var reportType = MediaType.valueOf(getAcceptHeader(request, MediaType.APPLICATION_XML_VALUE));
        return ResponseEntity.ok()
                .contentType(reportType)
                .body(outputStream -> {
                    if (MediaType.APPLICATION_JSON.equals(reportType)) {
                        writeReportAsJson(outputStream, report, domainConfig);
                    } else {
                        var wrapInputInCDATA = Objects.requireNonNullElse(in.getWrapReportDataInCDATA(), true);
                        fileManager.saveReport(report, outputStream, domainConfig, wrapInputInCDATA);
                    }
                });
    }

    /**
     * Execute the process to validate the content.
     *
     * @param in The input for the validation of one XML document.
     * @param domainConfig The validation domain.
     * @return The report.
     */
    private TAR executeValidationProcess(Input in, DomainConfig domainConfig) {
        var parentFolder = fileManager.createTemporaryFolderPath();
        var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(in.getLocale()), domainConfig));
        try {
            // Extract and validate inputs.
            var validationType = inputHelper.validateValidationType(domainConfig, in.getValidationType());
            var locationAsPath = Objects.requireNonNullElse(in.getLocationAsPath(), true);
            var addInputToReport = Objects.requireNonNullElse(in.getAddInputToReport(), false);
            var contentEmbeddingMethod = inputHelper.getEmbeddingMethod(in.getEmbeddingMethod());
            var externalSchemas = getExternalSchemas(domainConfig, in.getExternalSchemas(), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA, parentFolder);
            var externalSchematrons = getExternalSchemas(domainConfig, in.getExternalSchematrons(), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMATRON, parentFolder);
            var contentToValidate = inputHelper.validateContentToValidate(in.getContentToValidate(), contentEmbeddingMethod, parentFolder);
            // Validate.
            XMLValidator validator = ctx.getBean(XMLValidator.class, contentToValidate, validationType, externalSchemas, externalSchematrons, domainConfig, locationAsPath, addInputToReport, localiser);
            return validator.validateAll();
        } catch (ValidatorException | NotFoundException e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw e;
        } catch (Exception e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw new ValidatorException(e);
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Validate multiple inputs considering their settings and producing separate validation reports.
     *
     * @param domain The domain where the validator is executed.
     * @param inputs The input for the validation (content and metadata for one or more XML documents).
     * @param request The HTTP request.
     * @return The validation result.
     */
    @Operation(summary = "Validate multiple XML documents.", description="Validate multiple XML documents. The content for each instance can be provided either within the request as a BASE64 encoded string or remotely as a URL.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Output.class))) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @PostMapping(value = "/rest/{domain}/api/validateMultiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Output[] validateMultiple(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one or more XML documents).")
            @RequestBody Input[] inputs,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        var outputs = new ArrayList<Output>(inputs.length);
        for (Input input: inputs) {
            Output output = new Output();
            var report = executeValidationProcess(input, domainConfig);
            try (var bos = new ByteArrayOutputStream()) {
                var wrapInputInCDATA = Objects.requireNonNullElse(input.getWrapReportDataInCDATA(), true);
                fileManager.saveReport(report, bos, domainConfig, wrapInputInCDATA);
                output.setReport(Base64.getEncoder().encodeToString(bos.toByteArray()));
                outputs.add(output);
            } catch (IOException e) {
                throw new ValidatorException(e);
            }
        }
        return outputs.toArray(new Output[] {});
    }

}
