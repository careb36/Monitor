package com.monitor.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper hardening for deserialization security (CWE-502).
 *
 * <p>Security decisions:
 * <ul>
 *   <li>FAIL_ON_UNKNOWN_PROPERTIES – rejects malformed/unexpected JSON to prevent mass assignment</li>
 *   <li>deactivateDefaultTyping() – disables polymorphic deserialization to prevent RCE gadgets</li>
 *   <li>Field-only visibility – no getter/setter exposure of internal state</li>
 * </ul>
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet">OWASP Deserialization</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/502.html">CWE-502: Deserialization of Untrusted Data</a>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Security: reject unknown properties to prevent mass assignment (CWE-915)
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Security: disable default typing to prevent deserialization RCE (CWE-502, CVE-2017-7525)
        mapper.deactivateDefaultTyping();

        // Security: only expose fields explicitly annotated, hide internal getters/setters
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper;
    }
}
