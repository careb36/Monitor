package com.monitor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Security audit aspect unit tests.
 */
class RateLimitingFilterTest {

    @Test
    @DisplayName("RateLimitingFilter is instantiable with default config")
    void rateLimitingFilterExists() {
        RateLimitingFilter filter = new RateLimitingFilter(20, 20, 60);
        assertNotNull(filter);
    }

    @Test
    @DisplayName("SecurityAuditAspect is instantiable")
    void securityAuditAspectExists() {
        SecurityAuditAspect aspect = new SecurityAuditAspect();
        assertNotNull(aspect);
    }
}
