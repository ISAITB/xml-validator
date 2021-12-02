package eu.europa.ec.itb.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DomainConfigTest {

    @Test
    void testConfigCreation() {
        var config = new DomainConfig();
        assertNotNull(config);
    }

}
