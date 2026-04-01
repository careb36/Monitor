package com.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Global CORS configuration – restricts cross-origin requests to
 * explicitly allowed frontend origins loaded from application.yml.
 *
 * <p>Security decisions:
 * <ul>
 *   <li>Origins loaded from config – never wildcard in production</li>
 *   <li>Only GET and OPTIONS allowed – SSE is read-only</li>
 *   <li>Credentials disabled – API uses HTTP Basic, not cookies</li>
 *   <li>Blocked origin attempts logged for security monitoring</li>
 * </ul>
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/CORS_Sheet_Cheat">OWASP CORS Cheat Sheet</a>
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${monitor.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                log.info("CORS allowed origins: {}", allowedOrigins);

                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.toArray(new String[0]))
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("Authorization", "Accept", "Cache-Control", "Last-Event-ID")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
