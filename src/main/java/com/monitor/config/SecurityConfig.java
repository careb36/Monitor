package com.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 7.x configuration for the Monitor dashboard API.
 *
 * <p>Security decisions (OWASP-aligned):
 * <ul>
 *   <li>HTTP Basic for SSE endpoint – stateless, no CSRF needed for GET-only API</li>
 *   <li>BCrypt(12) for password hashing – NIST SP 800-63B compliant work factor</li>
 *   <li>Full security headers – HSTS, CSP, X-Content-Type-Options, Referrer-Policy</li>
 *   <li>Profile-aware – relaxed for dev, strict for staging/prod</li>
 * </ul>
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet">OWASP REST Security</a>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt with 12 rounds (~250ms per hash on modern hardware).
     * NIST SP 800-63B recommends at least 10 rounds; 12 provides extra margin.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Development profile: relaxed but still authenticated ──────────────────

    /**
     * Dev profile: authentication required on SSE endpoint, but relaxed for other paths.
     * Security headers are still applied for defense-in-depth.
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Stateless API
                .cors(Customizer.withDefaults()) // Uses CorsConfig bean
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/events/stream").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(Customizer.withDefaults()))
                .build();
    }

    // ── Staging/Production profile: strict security controls ──────────────────

    /**
     * Non-dev profile: all endpoints require authentication.
     * SSE stream requires MONITOR_USER role.
     * Admin endpoints require MONITOR_ADMIN role.
     * Full OWASP security header suite applied.
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Stateless API – no session-based CSRF
                .cors(Customizer.withDefaults()) // Uses CorsConfig bean
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/events/stream")
                            .hasRole("MONITOR_USER")
                        .requestMatchers("/api/admin/**")
                            .hasRole("MONITOR_ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // HSTS: 1 year, include subdomains
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Prevent MIME type sniffing (CWE-16)
                        .contentTypeOptions(Customizer.withDefaults())
                        // Deny framing – clickjacking prevention (CWE-1021)
                        .frameOptions(frame -> frame.deny())
                        // XSS protection for legacy browsers
                        .xssProtection(Customizer.withDefaults())
                        // Referrer policy – no full URL leakage
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // CSP – API-only service: block everything except self
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        // Permissions-Policy – disable all browser APIs
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()")))
                .build();
    }

    // ── User details: credentials from environment variables ──────────────────

    /**
     * In-memory user sourced from environment variables with Spring property fallback.
     * Priority: env var > spring property > fail-fast.
     *
     * <p>In production, credentials must be injected via:
     * <ul>
     *   <li>Docker: environment: section in docker-compose.yml</li>
     *   <li>Kubernetes: Secret resource mounted as env vars</li>
     *   <li>Shell: export MONITOR_USER=... MONITOR_PASSWORD=...</li>
     * </ul>
     */
    @Bean
    @Profile("!dev")
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${monitor.security.username:monitor}") String propertyUsername,
            @Value("${monitor.security.password:}") String propertyPassword) {

        // Env var takes precedence; fallback to Spring property
        String username = System.getenv().getOrDefault("MONITOR_USER", propertyUsername);
        String envPassword = System.getenv().get("MONITOR_PASSWORD");
        String rawPassword = (envPassword != null && !envPassword.isBlank())
                ? envPassword
                : propertyPassword;

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException(
                    "SECURITY: MONITOR_PASSWORD env var or monitor.security.password property must be set. "
                    + "Generate with: openssl rand -base64 32");
        }

        var monitorUser = User.builder()
                .username(username)
                .password(encoder.encode(rawPassword))
                .roles("MONITOR_USER")
                .build();

        return new InMemoryUserDetailsManager(monitorUser);
    }
}
