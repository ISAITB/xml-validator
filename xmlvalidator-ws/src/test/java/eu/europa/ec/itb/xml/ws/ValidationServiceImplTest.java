package eu.europa.ec.itb.xml.ws;

import com.gitb.core.TypedParameter;
import com.gitb.core.UsageEnumeration;
import com.gitb.core.ValidationModule;
import com.gitb.vs.Void;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.InputHelper;
import eu.europa.ec.itb.xml.validation.ValidationConstants;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ValidationServiceImplTest {

    @Test
    void testGetModuleDefinition() {
        var domainConfig = mock(DomainConfig.class);
        var inputHelper = mock(InputHelper.class);
        doReturn("domain1").when(domainConfig).getDomain();
        doReturn("service1").when(domainConfig).getWebServiceId();
        doReturn(true).when(domainConfig).hasMultipleValidationTypes();
        doReturn(true).when(inputHelper).supportsExternalArtifacts(any(), any());
        doAnswer((Answer<?>) ctx -> {
            var info = Map.of("type1", new TypedValidationArtifactInfo(), "type2", new TypedValidationArtifactInfo());
            info.get("type1").add(DomainConfig.ARTIFACT_TYPE_SCHEMA, new ValidationArtifactInfo());
            info.get("type1").get().setExternalArtifactSupport(ExternalArtifactSupport.NONE);
            info.get("type2").add(DomainConfig.ARTIFACT_TYPE_SCHEMA, new ValidationArtifactInfo());
            info.get("type2").get().setExternalArtifactSupport(ExternalArtifactSupport.NONE);
            return info;
        }).when(domainConfig).getArtifactInfo();
        doReturn(Map.ofEntries(
                descriptionEntryOf(ValidationConstants.INPUT_TYPE),
                descriptionEntryOf(ValidationConstants.INPUT_XML),
                descriptionEntryOf(ValidationConstants.INPUT_EMBEDDING_METHOD),
                descriptionEntryOf(ValidationConstants.INPUT_LOCATION_AS_PATH),
                descriptionEntryOf(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT),
                descriptionEntryOf(ValidationConstants.INPUT_SHOW_LOCATION_PATHS),
                descriptionEntryOf(ValidationConstants.INPUT_EXTERNAL_SCHEMA),
                descriptionEntryOf(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON),
                descriptionEntryOf(ValidationConstants.INPUT_LOCALE)
        )).when(domainConfig).getWebServiceDescription();
        var service = new ValidationServiceImpl(domainConfig);
        service.inputHelper = inputHelper;
        var result = service.getModuleDefinition(new Void());
        assertNotNull(result);
        assertNotNull(result.getModule());
        assertNotNull(result.getModule().getInputs());
        assertEquals(9, result.getModule().getInputs().getParam().size());
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_TYPE, result.getModule().getInputs().getParam().get(0), UsageEnumeration.R);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_XML, result.getModule().getInputs().getParam().get(1), UsageEnumeration.R);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_EMBEDDING_METHOD, result.getModule().getInputs().getParam().get(2), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_LOCATION_AS_PATH, result.getModule().getInputs().getParam().get(3), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, result.getModule().getInputs().getParam().get(4), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_SHOW_LOCATION_PATHS, result.getModule().getInputs().getParam().get(5), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_EXTERNAL_SCHEMA, result.getModule().getInputs().getParam().get(6), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_EXTERNAL_SCHEMATRON, result.getModule().getInputs().getParam().get(7), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_LOCALE, result.getModule().getInputs().getParam().get(8), UsageEnumeration.O);
    }

    private Map.Entry<String, String> descriptionEntryOf(String inputName) {
        return Map.entry(inputName, "Description of "+inputName);
    }

    private void assertWebServiceInputDocumentation(String inputName, TypedParameter parameter, UsageEnumeration usage) {
        assertEquals(inputName, parameter.getName());
        assertEquals("Description of "+inputName, parameter.getDesc());
        assertEquals(usage, parameter.getUse());
    }
}
