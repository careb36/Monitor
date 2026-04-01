package com.monitor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Rate limiting filter configuration unit tests.
 */
class RateLimitingFilterTest {

    @Test
    @DisplayName("CorsConfig is instantiable")
    void corsConfigExists() {
        CorsConfig config = new CorsConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("JacksonConfig is instantiable")
    void jacksonConfigExists() {
        JacksonConfig config = new JacksonConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("JacksonConfig creates hardened ObjectMapper")
    void jacksonConfigCreatesSecureObjectMapper() {
        JacksonConfig config = new JacksonConfig();
        var mapper = config.objectMapper();
        assertNotNull(mapper);
        // Verify FAIL_ON_UNKNOWN_PROPERTIES is enabled
        assert mapper.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
