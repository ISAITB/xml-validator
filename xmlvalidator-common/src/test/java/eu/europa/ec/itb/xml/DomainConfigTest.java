package eu.europa.ec.itb.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DomainConfigTest {

    @Test
    void testNewLabelConfig() {
        var config = new DomainConfig();
        var result = config.newLabelConfig();
        assertNotNull(result);
        assertEquals(DomainConfig.LabelConfig.class, result.getClass());
    }

}
