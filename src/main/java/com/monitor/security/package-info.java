/**
 * Security components for the Monitor platform.
 *
 * <ul>
 *   <li>{@link com.monitor.security.RateLimitingFilter} – per-IP Bucket4j token-bucket
 *       filter that prevents brute-force authentication attacks (CWE-307).</li>
 *   <li>{@link com.monitor.security.SecurityAuditAspect} – AOP aspect that logs
 *       endpoint access timing and errors to the {@code SECURITY_AUDIT} logger for
 *       SIEM integration (OWASP A09).</li>
 * </ul>
 */
package com.monitor.security;
