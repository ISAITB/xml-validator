package eu.europa.ec.itb.einvoice.ws;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.Void;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.validation.ValidationConstants;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
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
@Component
@Scope("prototype")
public class ValidationServiceImpl implements com.gitb.vs.ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
    ApplicationContext ctx;
    @Autowired
    ApplicationConfig config;

    public ValidationServiceImpl(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        MDC.put("domain", domainConfig.getDomainName());
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(domainConfig.getWebServiceId());
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(domainConfig.getWebServiceId());
        response.getModule().getMetadata().setVersion("1.0.0");
        response.getModule().setInputs(new TypedParameters());
        if (domainConfig.hasMultipleValidationTypes()) {
            TypedParameter xmlInput =  new TypedParameter();
            xmlInput.setName(ValidationConstants.INPUT_TYPE);
            xmlInput.setType("string");
            xmlInput.setUse(UsageEnumeration.R);
            xmlInput.setKind(ConfigurationType.SIMPLE);
            xmlInput.setDesc(domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_TYPE));
            response.getModule().getInputs().getParam().add(xmlInput);
        }
        TypedParameter xmlInput =  new TypedParameter();
        xmlInput.setName(ValidationConstants.INPUT_XML);
        xmlInput.setType("object");
        xmlInput.setUse(UsageEnumeration.R);
        xmlInput.setKind(ConfigurationType.SIMPLE);
        xmlInput.setDesc(domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_XML));
        response.getModule().getInputs().getParam().add(xmlInput);
        return response;
    }

    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
        MDC.put("domain", domainConfig.getDomainName());
        List<AnyContent> fileInputs = getXMLInput(validateRequest);
        String validationType = null;
        if (fileInputs.isEmpty()) {
            throw new IllegalArgumentException("You must provide the file to validate");
        }
        if (fileInputs.size() > 1) {
            throw new IllegalArgumentException("A single input file is expected");
        }
        if (domainConfig.hasMultipleValidationTypes()) {
            List<AnyContent> validationTypeInputs = getTypeInput(validateRequest);
            if (validationTypeInputs.isEmpty()) {
                throw new IllegalArgumentException("You must provide the type of invoice to perform");
            }
            if (validationTypeInputs.size() > 1) {
                throw new IllegalArgumentException("A single invoice type is expected");
            }
            validationType = validationTypeInputs.get(0).getValue();
            if (!domainConfig.getType().contains(validationType)) {
                throw new IllegalArgumentException("Invalid invoice type provided ["+validationType+"]");
            }
        }
        String invoiceToValidate;
        try {
            invoiceToValidate = extractContent(fileInputs.get(0)).trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read provided input", e);
        }
        XMLValidator validator;
        try {
            validator = ctx.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes("UTF-8")), validationType, domainConfig);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to decode inout as UTF-8 - using default [{}]", e.getMessage());
            validator = ctx.getBean(XMLValidator.class, new ByteArrayInputStream(invoiceToValidate.getBytes()), validationType, domainConfig);
        }
        TAR report = validator.validateAll();
        ValidationResponse result = new ValidationResponse();
        result.setReport(report);
        return result;
    }

    private List<AnyContent> getXMLInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_XML);
    }

    private List<AnyContent> getTypeInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_TYPE);
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
