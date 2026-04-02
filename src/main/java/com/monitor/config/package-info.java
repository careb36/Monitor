/**
 * Spring configuration classes for the Monitor application.
 *
 * <ul>
 *   <li>{@link com.monitor.config.SecurityConfig} – Spring Security 7.x configuration;
 *       profile-aware (dev vs prod), BCrypt(12), OWASP security headers.</li>
 *   <li>{@link com.monitor.config.CorsConfig} – CORS policy restricted to explicitly
 *       configured frontend origins.</li>
 *   <li>{@link com.monitor.config.JacksonConfig} – hardened Jackson {@code ObjectMapper}
 *       (no default typing, FAIL_ON_UNKNOWN_PROPERTIES, field-only visibility).</li>
 *   <li>{@link com.monitor.config.MonitorMailProperties} – {@code @ConfigurationProperties}
 *       binding for {@code monitor.mail.*} (from address, recipients, subject prefix).</li>
 *   <li>{@link com.monitor.config.MonitorPollingTargetsProperties} – {@code @ConfigurationProperties}
 *       binding for {@code monitor.polling.targets.*} (database and daemon names).</li>
 * </ul>
 */
package com.monitor.config;
