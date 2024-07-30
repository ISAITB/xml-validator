package eu.europa.ec.itb.xml.rest;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.BaseRestController;
import eu.europa.ec.itb.validation.commons.web.rest.model.ApiInfo;
import eu.europa.ec.itb.validation.commons.web.rest.model.Output;
import eu.europa.ec.itb.xml.*;
import eu.europa.ec.itb.xml.rest.model.ContextFileInfo;
import eu.europa.ec.itb.xml.rest.model.Input;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * REST controller to allow triggering the validator via its REST API.
 */
@Tag(name = "/rest/{domain}/api", description = "Operations for the validation of XML content against XML Schemas and Schematron.")
@RestController
public class RestValidationController extends BaseRestController<DomainConfig, ApplicationConfig, FileManager, InputHelper> {

    private static final Logger LOG = LoggerFactory.getLogger(RestValidationController.class);
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
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = MediaType.APPLICATION_XML_VALUE), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(value = "/rest/{domain}/api/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<StreamingResponseBody> validate(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.",
                    examples = {
                            @ExampleObject(name="order", summary="Sample 'order' configuration", value="order", description = "The domain value to use for the demo 'order' validator at https://www.itb.ec.europe.eu/order/upload."),
                            @ExampleObject(name="xml", summary="Generic 'xml' configuration", value = "xml", description = "The domain value to use for the generic 'xml' validator at https://www.itb.ec.europe.eu/xml/upload used to validate XML with user-provided XSDs and Schematron.")
                    }
            )
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one XML document).")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(name="order1", summary = "Validate string", description = "Validate content provided as a string for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "<?xml version=\\"1.0\\"?>\\n<purchaseOrder xmlns=\\"http:\\/\\/itb.ec.europa.eu\\/sample\\/po.xsd\\" orderDate=\\"2018-01-22\\">\\n  <shipTo country=\\"BE\\">\\n    <name>John Doe<\\/name>\\n    <street>Europa Avenue 123<\\/street>\\n    <city>Brussels<\\/city>\\n    <zip>1000<\\/zip>\\n  <\\/shipTo>\\n  <billTo country=\\"BE\\">\\n    <name>Jane Doe<\\/name>\\n    <street>Europa Avenue 210<\\/street>\\n    <city>Brussels<\\/city>\\n    <zip>1000<\\/zip>\\n  <\\/billTo>\\n  <comment>Send in one package please<\\/comment>\\n  <items>\\n    <item partNum=\\"XYZ-123876\\">\\n      <productName>Mouse<\\/productName>\\n      <quantity>20<\\/quantity>\\n      <priceEUR>15.99<\\/priceEUR>\\n      <comment>Confirm this is wireless<\\/comment>\\n    <\\/item>\\n    <item partNum=\\"ABC-32478\\">\\n      <productName>Keyboard<\\/productName>\\n      <quantity>15<\\/quantity>\\n      <priceEUR>25.50<\\/priceEUR>\\n    <\\/item>\\n  <\\/items>\\n<\\/purchaseOrder>",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="order2", summary = "Validate remote URI", description = "Validate content provided as a URI for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                        "embeddingMethod": "URL",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="order3", summary = "Validate Base64-encoded content", description = "Validate content encoded in a Base64 string for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "PHB1cmNoYXNlT3JkZXIgeG1sbnM9Imh0dHA6Ly9pdGIuZWMuZXVyb3BhLmV1L3NhbXBsZS9wby54c2QiIG9yZGVyRGF0ZT0iMjAxOC0wMS0yMiI+Cgk8c2hpcFRvIGNvdW50cnk9IkJFIj4KCQk8bmFtZT5Kb2huIERvZTwvbmFtZT4KCQk8c3RyZWV0PkV1cm9wYSBBdmVudWUgMTIzPC9zdHJlZXQ+CgkJPGNpdHk+QnJ1c3NlbHM8L2NpdHk+CgkJPHppcD4xMDAwPC96aXA+Cgk8L3NoaXBUbz4KCTxiaWxsVG8gY291bnRyeT0iQkUiPgoJCTxuYW1lPkphbmUgRG9lPC9uYW1lPgoJCTxzdHJlZXQ+RXVyb3BhIEF2ZW51ZSAyMTA8L3N0cmVldD4KCQk8Y2l0eT5CcnVzc2VsczwvY2l0eT4KCQk8emlwPjEwMDA8L3ppcD4KCTwvYmlsbFRvPgoJPGNvbW1lbnQ+U2VuZCBpbiBvbmUgcGFja2FnZSBwbGVhc2U8L2NvbW1lbnQ+Cgk8aXRlbXM+CgkJPGl0ZW0gcGFydE51bT0iWFlaLTEyMzg3NiI+CgkJCTxwcm9kdWN0TmFtZT5Nb3VzZTwvcHJvZHVjdE5hbWU+CgkJCTxxdWFudGl0eT41PC9xdWFudGl0eT4KCQkJPHByaWNlRVVSPjE1Ljk5PC9wcmljZUVVUj4KCQkJPGNvbW1lbnQ+Q29uZmlybSB0aGlzIGlzIHdpcmVsZXNzPC9jb21tZW50PgoJCTwvaXRlbT4KCQk8aXRlbSBwYXJ0TnVtPSJBQkMtMzI0NzgiPgoJCQk8cHJvZHVjdE5hbWU+S2V5Ym9hcmQ8L3Byb2R1Y3ROYW1lPgoJCQk8cXVhbnRpdHk+MTU8L3F1YW50aXR5PgoJCQk8cHJpY2VFVVI+MjUuNTA8L3ByaWNlRVVSPgoJCTwvaXRlbT4KCTwvaXRlbXM+CjwvcHVyY2hhc2VPcmRlcj4=",
                                        "embeddingMethod": "BASE64",
                                        "validationType": "large"
                                    }
                                    """),
                                    @ExampleObject(name="xml", summary = "Validate remote URI with user-provided XSD and Schematron", description = "Validate content provided as a URI and using a user-provided XSD and Schematron, with the generic 'xml' validator (see https://www.itb.ec.europe.eu/xml/upload). To try it out select also 'xml' for the 'domain' parameter.", value = """
                                    {
                                        "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                        "embeddingMethod": "URL",
                                        "externalSchemas": [
                                            {
                                                "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-xml-sample/master/resources/xsd/PurchaseOrder.xsd",
                                                "embeddingMethod": "URL"
                                            }
                                        ],
                                        "externalSchematrons": [
                                            {
                                                "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-xml-sample/master/resources/sch/LargePurchaseOrder.sch",
                                                "embeddingMethod": "URL"
                                            }
                                        ]
                                    }
                                    """)
                            }
                    )
            )
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
                        var wrapReportDataInCDATA = Objects.requireNonNullElse(in.getWrapReportDataInCDATA(), false);
                        fileManager.saveReport(report, outputStream, domainConfig, wrapReportDataInCDATA);
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
            var contextFiles = getContextFiles(domainConfig, in.getContextFiles(), validationType, parentFolder);
            var contentToValidate = inputHelper.validateContentToValidate(in.getContentToValidate(), contentEmbeddingMethod, null, parentFolder);
            // Validate.
            ValidationSpecs specs = ValidationSpecs.builder(contentToValidate, localiser, domainConfig, ctx)
                    .withValidationType(validationType)
                    .withExternalSchemas(externalSchemas)
                    .withExternalSchematrons(externalSchematrons)
                    .locationAsPath(locationAsPath)
                    .addInputToReport(addInputToReport)
                    .withContextFiles(contextFiles)
                    .withTempFolder(parentFolder.toPath())
                    .build();
            XMLValidator validator = ctx.getBean(XMLValidator.class, specs);
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
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.",
                    examples = {
                            @ExampleObject(name="order", summary="Sample 'order' configuration", value="order", description = "The domain value to use for the demo 'order' validator at https://www.itb.ec.europe.eu/order/upload."),
                            @ExampleObject(name="xml", summary="Generic 'xml' configuration", value = "xml", description = "The domain value to use for the generic 'xml' validator at https://www.itb.ec.europe.eu/xml/upload used to validate XML with user-provided XSDs and Schematron.")
                    }
            )
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one or more XML documents).")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(name="order", summary = "Validate remote URIs", description = "Validate content provided as URIs for the 'large' validation type of the 'order' sample validator (see https://www.itb.ec.europe.eu/order/upload). To try it out select also 'order' for the 'domain' parameter.", value = """
                                    [
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                            "embeddingMethod": "URL",
                                            "validationType": "large"
                                        },
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                            "embeddingMethod": "URL",
                                            "validationType": "basic"
                                        }
                                    ]
                                    """),
                                    @ExampleObject(name="xml", summary = "Validate remote URIs with user-provided XSD and Schematron", description = "Validate content provided as URIs and using user-provided XSD and Schematron, with the generic 'xml' validator (see https://www.itb.ec.europe.eu/xml/upload). To try it out select also 'xml' for the 'domain' parameter.", value = """
                                    [
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                            "embeddingMethod": "URL",
                                            "externalSchemas": [
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-xml-sample/master/resources/xsd/PurchaseOrder.xsd",
                                                    "embeddingMethod": "URL"
                                                }
                                            ]
                                        },
                                        {
                                            "contentToValidate": "https://www.itb.ec.europa.eu/files/samples/xml/sample-invalid.xml",
                                            "embeddingMethod": "URL",
                                            "externalSchemas": [
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-xml-sample/master/resources/xsd/PurchaseOrder.xsd",
                                                    "embeddingMethod": "URL"
                                                }
                                            ],
                                            "externalSchematrons": [
                                                {
                                                    "schema": "https://raw.githubusercontent.com/ISAITB/validator-resources-xml-sample/master/resources/sch/LargePurchaseOrder.sch",
                                                    "embeddingMethod": "URL"
                                                }
                                            ]
                                        }
                                    ]
                                    """)
                            }
                    )
            )
            @RequestBody Input[] inputs,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        var outputs = new ArrayList<Output>(inputs.length);
        for (Input input: inputs) {
            Output output = new Output();
            var report = executeValidationProcess(input, domainConfig);
            try (var bos = new ByteArrayOutputStream()) {
                var wrapReportDataInCDATA = Objects.requireNonNullElse(input.getWrapReportDataInCDATA(), false);
                fileManager.saveReport(report, bos, domainConfig, wrapReportDataInCDATA);
                output.setReport(Base64.getEncoder().encodeToString(bos.toByteArray()));
                outputs.add(output);
            } catch (IOException e) {
                throw new ValidatorException(e);
            }
        }
        return outputs.toArray(new Output[] {});
    }

    /**
     * Get the submitted context files.
     *
     * @param config The domain configuration.
     * @param receivedContextFiles The received context files.
     * @param validationType The validation type.
     * @param parentFolder The temporary folder to consider.
     * @return The list of context files.
     * @throws IOException If an IO error occurs.
     */
    private List<ContextFileData> getContextFiles(DomainConfig config, List<ContextFileInfo> receivedContextFiles, String validationType, File parentFolder) throws IOException {
        var contextFileConfigs = config.getContextFiles(validationType);
        if (contextFileConfigs.isEmpty()) {
            return Collections.emptyList();
        } else {
            int expectedContextFiles = contextFileConfigs.size();
            if (expectedContextFiles != receivedContextFiles.size()) {
                var exception = new ValidatorException("validator.label.exception.wrongContextFileCount");
                LOG.error(exception.getMessageForLog());
                throw exception;
            } else {
                int index = 0;
                List<ContextFileData> contextFiles = new ArrayList<>();
                for (var contextFileConfig: contextFileConfigs) {
                    var targetFile = Path.of(parentFolder.getPath(), "contextFiles").resolve(contextFileConfig.path()).toFile();
                    var receivedContextFile = receivedContextFiles.get(index).toFileContent();
                    if (receivedContextFile.getEmbeddingMethod() != null) {
                        switch (receivedContextFile.getEmbeddingMethod()) {
                            case BASE_64 -> fileManager.getFileFromBase64(targetFile.getParentFile(), receivedContextFile.getContent(), FileManager.EXTERNAL_FILE, targetFile.getName());
                            case URI -> fileManager.getFileFromURL(targetFile.getParentFile(), receivedContextFile.getContent(), "", targetFile.getName());
                            default -> fileManager.getFileFromString(targetFile.getParentFile(), receivedContextFile.getContent(), FileManager.EXTERNAL_FILE, targetFile.getName());
                        }
                    } else {
                        fileManager.getFileFromURLOrBase64(targetFile.getParentFile(), receivedContextFile.getContent(), null, null, targetFile.getName());
                    }
                    contextFiles.add(new ContextFileData(targetFile.toPath(), contextFileConfig));
                    index += 1;
                }
                return contextFiles;
            }
        }
    }

}
