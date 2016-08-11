package eu.europa.ec.itb.einvoice.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.types.DataType;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import java.io.*;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by simatosc on 25/02/2016.
 */
@WebService(
        name = "ValidationService",
        serviceName = "ValidationService",
        targetNamespace = "http://www.gitb.com/vs/v1/",
        endpointInterface = "com.gitb.vs.ValidationService"
)
public class ValidationService extends SpringBeanAutowiringSupport implements com.gitb.vs.ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    public static String INPUT_TYPE = "type";
    public static String INPUT_XML = "xml";

    @Resource
    ServletContext ctx;

    ApplicationContext beans;
    ApplicationConfig config;

    @PostConstruct
    public void init() {
        beans = WebApplicationContextUtils.getRequiredWebApplicationContext(ctx);
        config = beans.getBean(ApplicationConfig.class);
    }

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(config.getWebServiceId());
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(config.getWebServiceId());
        response.getModule().getMetadata().setVersion("1.0.0");
        response.getModule().setInputs(new TypedParameters());
        if (config.hasMultipleValidationTypes()) {
            TypedParameter xmlInput =  new TypedParameter();
            xmlInput.setName(INPUT_TYPE);
            xmlInput.setType(DataType.STRING_DATA_TYPE);
            xmlInput.setUse(UsageEnumeration.R);
            xmlInput.setKind(ConfigurationType.SIMPLE);
            xmlInput.setDesc(config.getWebServiceDescription().get(INPUT_TYPE));
            response.getModule().getInputs().getParam().add(xmlInput);
        }
        TypedParameter xmlInput =  new TypedParameter();
        xmlInput.setName(INPUT_XML);
        xmlInput.setType(DataType.OBJECT_DATA_TYPE);
        xmlInput.setUse(UsageEnumeration.R);
        xmlInput.setKind(ConfigurationType.SIMPLE);
        xmlInput.setDesc(config.getWebServiceDescription().get(INPUT_XML));
        response.getModule().getInputs().getParam().add(xmlInput);
        return response;
    }

    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        List<AnyContent> fileInputs = getXMLInput(validateRequest);
        String validationType = null;
        if (fileInputs.isEmpty()) {
            throw new IllegalArgumentException("You must provide the file to validate");
        }
        if (fileInputs.size() > 1) {
            throw new IllegalArgumentException("A single input file is expected");
        }
        if (config.hasMultipleValidationTypes()) {
            List<AnyContent> validationTypeInputs = getTypeInput(validateRequest);
            if (validationTypeInputs.isEmpty()) {
                throw new IllegalArgumentException("You must provide the type of validation to perform");
            }
            if (validationTypeInputs.size() > 1) {
                throw new IllegalArgumentException("A single validation type is expected");
            }
            validationType = validationTypeInputs.get(0).getValue();
            if (!config.getType().contains(validationType)) {
                throw new IllegalArgumentException("Invalid validation type provided ["+validationType+"]");
            }
        }
        String invoiceToValidate = null;
        try {
            invoiceToValidate = extractContent(fileInputs.get(0)).trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read provided input", e);
        }
        XMLValidator validator;
        try {
            validator = beans.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes("UTF-8")), validationType);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to decode inout as UTF-8 - using default [{}]", e.getMessage());
            validator = beans.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes()), validationType);
        }
        TAR report = validator.validateAll();
        ValidationResponse result = new ValidationResponse();
        result.setReport(report);
        return result;
    }

    private List<AnyContent> getXMLInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, INPUT_XML);
    }

    private List<AnyContent> getTypeInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, INPUT_TYPE);
    }

    private List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<AnyContent>();
        if (validateRequest != null) {
            if (validateRequest.getInput() != null) {
                for (AnyContent anInput: validateRequest.getInput()) {
                    if (name.equals(anInput.getName())) {
                        inputs.add(anInput);
                    }
                }
            }
        }
        return inputs;
    }

    /**
     * Extract the String represented by the provided content.
     *
     * @param content The content to process.
     * @return The content's String representation.
     */
    private String extractContent(AnyContent content) throws IOException {
        String stringContent = null;
        if (content != null && content.getValue() != null) {
            switch (content.getEmbeddingMethod()) {
                case STRING:
                    // Use string as-is.
                    stringContent = content.getValue();
                    break;
                case URI:
                    // Read the string from the provided URI.
                    URI uri = URI.create(content.getValue());
                    Proxy proxy = null;
                    List<Proxy> proxies = ProxySelector.getDefault().select(uri);
                    if (proxies != null && !proxies.isEmpty()) {
                        proxy = proxies.get(0);
                    }
                    BufferedReader in = null;
                    try {
                        URLConnection connection;
                        if (proxy == null) {
                            connection = uri.toURL().openConnection();
                        } else {
                            connection = uri.toURL().openConnection(proxy);
                        }
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder builder = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            builder.append(inputLine);
                        }
                        stringContent = builder.toString();
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Unable to read provided URI", e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                // Ignore.
                            }
                        }
                    }
                    break;
                default: // BASE_64
                    // Construct the string from its BASE64 encoded bytes.
                    char[] buffer = new char[1024];
                    int numCharsRead;
                    StringBuilder sb = new StringBuilder();
                    try (BomStrippingReader reader = new BomStrippingReader(new ByteArrayInputStream(Base64.decodeBase64(content.getValue())))) {
                        while ((numCharsRead = reader.read(buffer, 0, buffer.length)) != -1) {
                            sb.append(buffer, 0, numCharsRead);
                        }
                    }
                    stringContent = sb.toString();
                    break;
            }
        }
        return stringContent;
    }

}
