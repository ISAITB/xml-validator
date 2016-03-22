package eu.europa.ec.itb.einvoice.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
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
import java.util.List;

/**
 * Created by simatosc on 25/02/2016.
 */
@WebService(endpointInterface = "com.gitb.vs.ValidationService")
public class ValidationService extends SpringBeanAutowiringSupport implements com.gitb.vs.ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private static String INPUT_XML = "xml";

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
        TypedParameter xmlInput =  new TypedParameter();
        xmlInput.setName(INPUT_XML);
        xmlInput.setUse(UsageEnumeration.R);
        xmlInput.setKind(ConfigurationType.SIMPLE);
        xmlInput.setDesc(config.getWebServiceDescription());
        response.getModule().getInputs().getParam().add(xmlInput);
        return response;
    }

    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        if (validateRequest == null
                || validateRequest.getInput() == null
                || validateRequest.getInput().isEmpty()) {
            throw new IllegalArgumentException("You must provide the file to validate");
        }
        if (validateRequest.getInput().size() > 1) {
            throw new IllegalArgumentException("A single input file is expected");
        }
        String invoiceToValidate = extractContent(validateRequest.getInput().get(0)).trim();
        XMLValidator validator;
        try {
            validator = beans.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to decode inout as UTF-8 - using default [{}]", e.getMessage());
            validator = new XMLValidator(new ByteArrayInputStream(invoiceToValidate.getBytes()));
        }
        TAR report = validator.validateAll();
        ValidationResponse result = new ValidationResponse();
        result.setReport(report);
        return result;
    }

    /**
     * Extract the String represented by the provided content.
     *
     * @param content The content to process.
     * @return The content's String representation.
     */
    private String extractContent(AnyContent content) {
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
                    stringContent = new String(Base64.decodeBase64(content.getValue()));
                    break;
            }
        }
        return stringContent;
    }

}
