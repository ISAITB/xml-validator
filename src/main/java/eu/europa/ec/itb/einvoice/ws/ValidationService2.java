package eu.europa.ec.itb.einvoice.ws;

import com.gitb.vs.*;
import com.gitb.vs.Void;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import javax.jws.WebParam;

/**
 * Created by simatosc on 29/02/2016.
 */
@Endpoint
public class ValidationService2 implements com.gitb.vs.ValidationService {

    public static final String NAMESPACE_URI = "http://www.gitb.com/vs/v1/";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetModuleDefinitionRequest")
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@RequestPayload Void parameters) {
        return null;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ValidateRequest")
    @Override
    public ValidationResponse validate(@RequestPayload ValidateRequest parameters) {
        return null;
    }
}
