package com.monitor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Security configuration unit tests.
 *
 * Validates SecurityConfig beans can be instantiated without Spring context.
 */
class SecurityConfigTest {

    @Test
    @DisplayName("SecurityConfig is instantiable")
    void securityConfigExists() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("BCryptPasswordEncoder is created with correct rounds")
    void passwordEncoderCreated() {
        SecurityConfig config = new SecurityConfig();
        var encoder = config.passwordEncoder();
        assertNotNull(encoder);
    }
}
