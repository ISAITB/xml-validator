package eu.europa.ec.itb.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainConfigTest {

    @Test
    void testConfigCreation() {
        var config = new DomainConfig();
        assertNotNull(config);
    }

}
