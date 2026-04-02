# INFORME DE AUDITORÍA DE SEGURIDAD – PROYECTO MONITOR

> **⚠️ Estado del documento: ANÁLISIS PRE-IMPLEMENTACIÓN**
>
> Este informe fue redactado **antes** de que se implementaran las medidas de seguridad.
> Los hallazgos describen vulnerabilidades que **existían** al momento de la auditoría.
> Muchas ya fueron resueltas (Spring Security, credenciales externalizadas, etc.).
> Para el estado **actual** de seguridad implementada, ver `SECURITY-CONFIG.md`.
>
> **Versiones reales:** Spring Boot 3.4.4, Java 21

**Fecha:** 2026-03-31
**Analista:** AppSec Senior Auditor
**Alcance:** Monitoreo de operaciones en tiempo real – Spring Boot backend
**Metodología:** OWASP Top 10 (2021) / CWE/SANS Top 25 (2023) / CVSS v3.1

---

## PREFACIO: RECONOCIMIENTO INICIAL

| Categoría | Valor |
|-----------|-------|
| **Estructura de módulos** | Monolito único (single `pom.xml`, sin módulos hijos) |
| **Spring Boot** | 3.4.4 (parent: `spring-boot-starter-parent`) |
| **Spring Framework** | 6.x (implícito por Spring Boot 3.4.x) |
| **JDK objetivo** | Java 21 (`<java.version>21</java.version>`, pom.xml:25) |
| **Motor de base de datos** | Oracle XE 11g (`gvenzl/oracle-xe:11`) |
| **Perfil Spring activo** | Ninguno explícito (default profile) |
| **Mensajería** | Apache Kafka via kafka-clients 4.2.0 + Debezium 3.5.0.Final |
| **Serialización JSON** | Jackson (managed by Spring Boot) |
| **Backend de persistencia** | Spring Data JPA (H2 embebido por defecto; Oracle vía Debezium CDC) |
| **Ausencias críticas** | Sin Spring Security, sin Spring Boot Actuator, sin dependency-check, sin enforcer-plugin |

---

## HALLAZGOS

---

### HALLAZGO #1 — SEVERIDAD: CRITICAL — CWE-306 — CVSS 9.8

**Superficie de ataque:** A01 Broken Access Control

📁 **Archivo:** `pom.xml`
📍 **Línea(s):** 31-83 (sección `<dependencies>`)

🔍 **Código vulnerable:**
```xml
<!-- Ausencia total de spring-boot-starter-security -->
<!-- No existe ningún mecanismo de autenticación/autorización -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- ... sin spring-boot-starter-security ... -->
</dependencies>
```

⚠️ **Riesgo técnico:**
El endpoint SSE `GET /api/events/stream` está expuesto públicamente sin autenticación alguna. Cualquier cliente de red puede suscribirse y recibir todos los eventos de monitoreo, incluyendo alertas CRITICAL con información sensible de infraestructura interna (nombres de servidores Oracle, estados de daemons, códigos de error). Esto constituye una vulnerabilidad de **Broken Access Control** (OWASP A01:2021) de máxima severidad. Un atacante externo puede:
1. Obtener inteligencia del estado de la infraestructura para planificar ataques dirigidos
2. Detectar cuándo sistemas están caídos (ventana de ataque óptima)
3. Enumerar la topología interna (nombres de bases de datos, servicios)

📋 **Referencia:** CWE-306 (Missing Authentication for Critical Function), OWASP A01:2021, CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H

✅ **Código refactorizado (Java 21 / Jakarta EE 11 / Spring Boot 4.x):**

**pom.xml — agregar dependencia:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Nuevo archivo `src/main/java/com/monitor/config/SecurityConfig.java`:**
```java
package com.monitor.config;

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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/events/stream").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
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
                .build();
    }

    @Bean
    @Profile("!dev")
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var user = User.builder()
                .username("${MONITOR_USER:monitor}")
                .password(encoder.encode("${MONITOR_PASSWORD:}"))
                .roles("MONITOR_USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

📋 **Validación:**
1. Acceder a `curl http://localhost:8080/api/events/stream` debe retornar `401 Unauthorized`
2. `curl -u monitor:password http://localhost:8080/api/events/stream` debe establecer la conexión SSE
3. Ejecutar `mvn --batch-mode clean verify` debe pasar todos los tests

---

### HALLAZGO #2 — SEVERIDAD: CRITICAL — CWE-798 — CVSS 9.1

**Superficie de ataque:** A07 Identification and Authentication Failures

📁 **Archivo:** `docker-compose.yml`
📍 **Línea(s):** 22

🔍 **Código vulnerable:**
```yaml
environment:
  ORACLE_PASSWORD: admin123    # ← Credencial hardcodeada
```

⚠️ **Riesgo técnico:**
La contraseña del usuario SYS de Oracle está hardcodeada como `admin123` en el archivo docker-compose.yml. Este archivo está versionado en Git. Cualquier persona con acceso al repositorio obtiene credenciales de superusuario de la base de datos Oracle. En un despliegue real, permite:
1. Acceso completo como SYSDBA a la base de datos
2. Lectura/escritura/modificación de todos los datos
3. Ejecución de comandos SQL arbitrarios con privilegios máximos
4. Acceso a LogMiner y datos de replicación CDC

📋 **Referencia:** CWE-798 (Use of Hard-coded Credentials), OWASP A07:2021

✅ **Código refactorizado:**

**docker-compose.yml — línea 22:**
```yaml
environment:
  ORACLE_PASSWORD: ${ORACLE_PASSWORD:?FATAL: Set ORACLE_PASSWORD env var before starting}
```

**Complementar con `.env.example`:**
```bash
# .env.example — commit este archivo como plantilla
# Copiar a .env y establecer valores reales (NUNCA commitear .env)
ORACLE_PASSWORD=changeme-in-production
MAIL_PASSWORD=changeme-in-production
```

📋 **Validación:**
1. Ejecutar `docker compose up` sin `ORACLE_PASSWORD` definida debe fallar con error claro
2. `grep -r "admin123" .` no debe retornar resultados
3. `.env` debe estar en `.gitignore`

---

### HALLAZGO #3 — SEVERIDAD: CRITICAL — CWE-798 — CVSS 8.6

**Superficie de ataque:** A07 Identification and Authentication Failures

📁 **Archivo:** `docker/oracle/init.sql`
📍 **Línea(s):** 22

🔍 **Código vulnerable:**
```sql
CREATE USER monitor_app IDENTIFIED BY monitor_pass
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;
```

⚠️ **Riesgo técnico:**
Credenciales de la aplicación Oracle hardcodeadas en script SQL versionado. El usuario `monitor_app` tiene credenciales predecibles (`monitor_pass`) que se repiten en `connector-log-traza.json:10`. Cualquier persona con acceso al repositorio puede conectarse a la base de datos con estos privilegios.

📋 **Referencia:** CWE-798 (Use of Hard-coded Credentials), CWE-259 (Use of Hard-coded Password)

✅ **Código refactorizado:**

**docker/oracle/init.sql — línea 22:**
```sql
-- Credenciales pasadas vía variable de entorno del contenedor
CREATE USER &MONITOR_APP_USER IDENTIFIED BY &MONITOR_APP_PASS
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;
```

**docker-compose.yml — agregar al servicio oracle:**
```yaml
environment:
  ORACLE_PASSWORD: ${ORACLE_PASSWORD:?FATAL: Set ORACLE_PASSWORD}
  MONITOR_APP_USER: monitor_app
  MONITOR_APP_PASS: ${MONITOR_APP_PASSWORD:?FATAL: Set MONITOR_APP_PASSWORD}
```

**docker/debezium/connector-log-traza.json — línea 9-10:**
```json
{
  "database.user": "${MONITOR_APP_USER}",
  "database.password": "${MONITOR_APP_PASSWORD}"
}
```

📋 **Validación:**
1. `grep -r "monitor_pass" .` no debe retornar resultados
2. El connector Debezium debe poder autenticarse con las variables de entorno

---

### HALLAZGO #4 — SEVERIDAD: HIGH — CWE-798 — CVSS 7.5

**Superficie de ataque:** A07 Identification and Authentication Failures

📁 **Archivo:** `src/main/resources/application.yml`
📍 **Línea(s):** 22

🔍 **Código vulnerable:**
```yaml
password: ${MAIL_PASSWORD:changeme}
```

⚠️ **Riesgo técnico:**
La contraseña SMTP tiene un default fallback hardcodeado `changeme`. Si la variable de entorno `MAIL_PASSWORD` no está configurada, la aplicación usa una contraseña trivial y predecible. Esto:
1. Permite a atacantes enviar emails como el servicio de monitoreo (spoofing de alertas)
2. Puede usarse para enviar spam o phishing usando la infraestructura corporativa
3. En combinación con `auth: false` (línea 26) y `starttls.enable: false` (línea 28), las credenciales se envían en texto plano por la red

📋 **Referencia:** CWE-798, OWASP A07:2021

✅ **Código refactorizado:**
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.groupwise.example.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

📋 **Validación:**
1. Ejecutar sin `MAIL_PASSWORD` debe causar `BeanCreationException` o error de conexión SMTP
2. `grep "changeme" src/` no debe retornar resultados

---

### HALLAZGO #5 — SEVERIDAD: HIGH — CWE-942 — CVSS 7.3

**Superficie de ataque:** A05 Security Misconfiguration

📁 **Archivo:** `src/main/java/com/monitor/config/CorsConfig.java`
📍 **Línea(s):** 22-23

🔍 **Código vulnerable:**
```java
registry.addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
```

⚠️ **Riesgo técnico:**
Configuración CORS que permite **cualquier origen** acceder a **todos los endpoints**. Aunque `allowCredentials` es `false`, esto permite que cualquier sitio web malicioso:
1. Consuma el endpoint SSE desde el navegador de una víctima autenticada (si se agrega autenticación más tarde)
2. Realice ataques de lectura cross-origin contra la API
3. El wildcard `allowedOriginPatterns("*")` es especialmente peligroso porque es funcionalmente equivalente a `allowedOrigins("*")` pero evita la restricción de `allowCredentials(true)` en configuraciones futuras

📋 **Referencia:** CWE-942 (Permissive Cross-domain Policy with Untrusted Domains), OWASP A05:2021

✅ **Código refactorizado:**
```java
package com.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${monitor.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("Authorization", "Accept", "Cache-Control", "Last-Event-ID")
                        .maxAge(3600);
            }
        };
    }
}
```

**application.yml:**
```yaml
monitor:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

📋 **Validación:**
1. `curl -H "Origin: http://evil.com" http://localhost:8080/api/events/stream -I` no debe retornar `Access-Control-Allow-Origin: http://evil.com`
2. `curl -H "Origin: http://localhost:3000" http://localhost:8080/api/events/stream -I` debe retornar `Access-Control-Allow-Origin: http://localhost:3000`

---

### HALLAZGO #6 — SEVERIDAD: HIGH — CWE-319 — CVSS 7.4

**Superficie de ataque:** A02 Cryptographic Failures

📁 **Archivo:** `src/main/resources/application.yml`
📍 **Línea(s):** 26-28

🔍 **Código vulnerable:**
```yaml
properties:
  mail:
    smtp:
      auth: false
      starttls:
        enable: false
```

⚠️ **Riesgo técnico:**
Autenticación SMTP completamente deshabilitada y TLS desactivado. Los emails de alerta CRITICAL se envían en texto plano por la red, exponiendo:
1. Contenido de alertas con información de infraestructura interna
2. Dirección del remitente y destinatarios
3. Cualquier credencial que eventualmente se configure (cuando se corrija el hallazgo #4)

📋 **Referencia:** CWE-319 (Cleartext Transmission of Sensitive Information), OWASP A02:2021

✅ **Código refactorizado:**
```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

📋 **Validación:**
1. Capturar tráfico SMTP con `tcpdump` o Wireshark: no debe haber texto plano
2. `grep "auth: false" src/main/resources/application.yml` no debe retornar resultados

---

### HALLAZGO #7 — SEVERIDAD: HIGH — CWE-693 — CVSS 7.0

**Superficie de ataque:** A06 Vulnerable and Outdated Components

📁 **Archivo:** `pom.xml`
📍 **Línea(s):** 85-96 (sección `<build><plugins>`)

🔍 **Código vulnerable:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

⚠️ **Riesgo técnico:**
Ausencia total de `org.owasp:dependency-check-maven` y `maven-enforcer-plugin`. Sin escaneo de vulnerabilidades en dependencias transitivas, el proyecto puede incorporar bibliotecas con CVEs críticos (ej. jackson-databind CVE-2022-42003, CVE-2020-36518) sin detección. Sin enforcer, no se previenen clases duplicadas ni dependencias snapshot en producción.

📋 **Referencia:** CWE-1104 (Use of Unmaintained Third Party Components), OWASP A06:2021

✅ **Código refactorizado:**
```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <dependency-check.version>10.0.3</dependency-check.version>
    <enforcer.version>3.5.0</enforcer.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
        </plugin>

        <!-- OWASP Dependency-Check -->
        <plugin>
            <groupId>org.owasp</groupId>
            <artifactId>dependency-check-maven</artifactId>
            <version>${dependency-check.version}</version>
            <configuration>
                <failBuildOnCVSS>7.0</failBuildOnCVSS>
                <format>HTML</format>
                <suppressionFiles>
                    <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
                </suppressionFiles>
                <assemblyAnalyzerEnabled>false</assemblyAnalyzerEnabled>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>

        <!-- Maven Enforcer -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <version>${enforcer.version}</version>
            <executions>
                <execution>
                    <id>enforce</id>
                    <goals>
                        <goal>enforce</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <requireJavaVersion>
                                <version>[21,)</version>
                            </requireJavaVersion>
                            <requireMavenVersion>
                                <version>[3.9,)</version>
                            </requireMavenVersion>
                            <requireReleaseDeps>
                                <failWhenParentIsSnapshot>true</failWhenParentIsSnapshot>
                            </requireReleaseDeps>
                            <banDuplicateClasses>
                                <findAllDuplicates>true</findAllDuplicates>
                            </banDuplicateClasses>
                        </rules>
                        <fail>true</fail>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Crear `dependency-check-suppressions.xml` en la raíz del proyecto:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Suprimir falsos positivos conocidos aquí, documentando la razón -->
    <!-- Ejemplo:
    <suppress>
        <notes><![CDATA[
            CVE-XXXX-XXXX does not apply because this project does not use the affected code path.
        ]]></notes>
        <packageUrl regex="true">^pkg:maven/org\.example/artifact@.*$</packageUrl>
        <cve>CVE-XXXX-XXXX</cve>
    </suppress>
    -->
</suppressions>
```

📋 **Validación:**
1. `mvn verify` debe ejecutar dependency-check y enforcer automáticamente
2. `mvn dependency-check:check` debe generar reporte HTML en `target/dependency-check-report.html`
3. Dependencias snapshot deben causar build failure

---

### HALLAZGO #8 — SEVERIDAD: HIGH — CWE-693 — CVSS 7.0

**Superficie de ataque:** A06 Vulnerable and Outdated Components

📁 **Archivo:** `.github/workflows/deploy.yml`
📍 **Línea(s):** 29

🔍 **Código vulnerable:**
```yaml
- name: Build production artifact
  run: mvn --batch-mode clean package -DskipTests
```

⚠️ **Riesgo técnico:**
El pipeline de producción omite **todos los tests** (incluyendo security tests). Una dependencia vulnerable o código con inyección SQL puede pasar a producción sin detección. Esto invalida cualquier gate de calidad previo al despliegue.

📋 **Referencia:** CWE-693 (Failure to Follow Specification of Security Controls)

✅ **Código refactorizado:**
```yaml
- name: Build production artifact
  run: mvn --batch-mode clean verify -Powasp
```

📋 **Validación:**
1. Hacer push a `main` con un test que falle debe rechazar el build de producción
2. El reporte de dependency-check debe generarse antes del empaquetado

---

### HALLAZGO #9 — SEVERIDAD: MEDIUM — CWE-502 — CVSS 5.9

**Superficie de ataque:** A08 Software and Data Integrity Failures

📁 **Archivo:** `src/main/java/com/monitor/config/JacksonConfig.java`
📍 **Línea(s):** 13-17

🔍 **Código vulnerable:**
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return objectMapper;
}
```

⚠️ **Riesgo técnico:**
Aunque no se encontró uso de `enableDefaultTyping()` ni `activateDefaultTyping()`, la configuración actual no establece explícitamente:
1. `FAIL_ON_UNKNOWN_PROPERTIES` (Spring Boot lo establece a `true` por defecto, pero el bean personalizado lo sobreescribe sin verificar)
2. No deshabilita explícitamente el default typing como defensa en profundidad
3. No registra un `PolymorphicTypeValidator` restrictivo

Si en el futuro se agrega deserialización polimórfica, la ausencia de un PTV puede llevar a RCE vía gadgets (CVE-2019-12384, CVE-2022-42003).

📋 **Referencia:** CWE-502 (Deserialization of Untrusted Data), OWASP A08:2021

✅ **Código refactorizado:**

**Variante segura (sin polimorfismo — recomendada):**
```java
package com.monitor.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.deactivateDefaultTyping(); // Explícitamente deshabilitado
        return mapper;
    }
}
```

📋 **Validación:**
1. Enviar JSON con propiedades desconocidas debe fallar con `UnrecognizedPropertyException`
2. Enviar JSON con `@class` debe ser ignorado silenciosamente

---

### HALLAZGO #10 — SEVERIDAD: MEDIUM — CWE-250 — CVSS 6.5

**Superficie de ataque:** A01 Broken Access Control

📁 **Archivo:** `docker/oracle/init.sql`
📍 **Línea(s):** 31-34

🔍 **Código vulnerable:**
```sql
GRANT EXECUTE_CATALOG_ROLE TO monitor_app;
GRANT SELECT ANY TRANSACTION TO monitor_app;
GRANT SELECT ANY TABLE TO monitor_app;
GRANT LOGMINING TO monitor_app;
```

⚠️ **Riesgo técnico:**
El usuario `monitor_app` tiene privilegios excesivos. `SELECT ANY TABLE` permite leer **todas** las tablas de **todos** los esquemas, no solo las tablas de monitoreo. `EXECUTE_CATALOG_ROLE` permite ejecutar paquetes del catálogo. Esto viola el principio de mínimo privilegio. Si las credenciales del usuario son comprometidas (ver hallazgo #3), el atacante tiene acceso de lectura completo a toda la base de datos.

📋 **Referencia:** CWE-250 (Execution with Unnecessary Privileges), OWASP A01:2021

✅ **Código refactorizado:**
```sql
-- Otorgar solo los privilegios mínimos necesarios para Debezium LogMiner
GRANT CREATE SESSION TO monitor_app;
GRANT SELECT ON SYS.V_$DATABASE TO monitor_app;
GRANT SELECT ON SYS.V_$LOG TO monitor_app;
GRANT SELECT ON SYS.V_$LOGFILE TO monitor_app;
GRANT SELECT ON SYS.V_$LOGMNR_CONTENTS TO monitor_app;
GRANT SELECT ON SYS.V_$ARCHIVED_LOG TO monitor_app;
GRANT SELECT ON SYS.V_$TRANSACTION TO monitor_app;
GRANT SELECT ANY TABLE TO monitor_app;
```

📋 **Validación:**
1. `SELECT * FROM dba_tab_privs WHERE grantee = 'MONITOR_APP'` debe mostrar solo privilegios mínimos
2. Intentar leer tablas de otros esquemas debe fallar si se revoca `SELECT ANY TABLE`

---

### HALLAZGO #11 — SEVERIDAD: MEDIUM — CWE-200 — CVSS 5.3

**Superficie de ataque:** A01 Broken Access Control

📁 **Archivo:** `docker-compose.yml`
📍 **Línea(s):** 23-24, 44-45, 66-67, 97-98

🔍 **Código vulnerable:**
```yaml
# Oracle
ports:
  - "1521:1521"
# Zookeeper
ports:
  - "2181:2181"
# Kafka
ports:
  - "9092:9092"
# Kafka Connect
ports:
  - "8083:8083"
```

⚠️ **Riesgo técnico:**
Todos los puertos de servicios internos están expuestos al host (y potencialmente a la red externa). La base de datos Oracle, Kafka, Zookeeper y Kafka Connect son accesibles desde fuera de la red Docker. En un entorno de desarrollo esto es conveniente, pero en producción esto es una superficie de ataque innecesaria.

📋 **Referencia:** CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor)

✅ **Código refactorizado:**
```yaml
# Solo exponer puertos necesarios; el resto accesible solo dentro de la red Docker
services:
  oracle:
    ports:
      - "${ORACLE_PORT:-1521}:1521"
  zookeeper:
    # No exponer al host — solo Kafka necesita acceso
  kafka:
    ports:
      - "${KAFKA_PORT:-9092}:9092"
  kafka-connect:
    ports:
      - "${CONNECT_PORT:-8083}:8083"
```

📋 **Validación:**
1. `docker compose ps` debe mostrar puertos solo según variables de entorno
2. `nmap localhost` no debe mostrar puertos innecesarios abiertos

---

### HALLAZGO #12 — SEVERIDAD: MEDIUM — CWE-327 — CVSS 5.3

**Superficie de ataque:** A02 Cryptographic Failures

📁 **Archivo:** `docker-compose.yml`
📍 **Línea(s):** 59-62

🔍 **Código vulnerable:**
```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
```

⚠️ **Riesgo técnico:**
Todas las comunicaciones Kafka (productores, consumidores, inter-broker) usan PLAINTEXT sin cifrado TLS ni autenticación SASL. Cualquier actor con acceso a la red Docker puede:
1. Leer todos los mensajes CDC (incluyendo datos de la tabla log_traza)
2. Inyectar mensajes maliciosos en los topics
3. Modificar mensajes en tránsito (ataque MITM)

📋 **Referencia:** CWE-327 (Use of a Broken or Risky Cryptographic Algorithm)

✅ **Código refactorizado:**
```yaml
KAFKA_ADVERTISED_LISTENERS: SASL_SSL://kafka:29092,SASL_SSL_HOST://localhost:9092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: SASL_SSL:SASL_SSL,SASL_SSL_HOST:SASL_SSL
KAFKA_INTER_BROKER_LISTENER_NAME: SASL_SSL
KAFKA_SASL_ENABLED_MECHANISMS: SCRAM-SHA-512
```

📋 **Validación:**
1. `kafka-console-consumer --bootstrap-server localhost:9092` sin credenciales debe fallar
2. Capturar tráfico con `tcpdump` debe mostrar datos cifrados

---

### HALLAZGO #13 — SEVERIDAD: LOW — CWE-1104 — CVSS 3.7

**Superficie de ataque:** A06 Vulnerable and Outdated Components

📁 **Archivo:** `pom.xml`
📍 **Línea(s):** 31-83

🔍 **Código vulnerable:**
```xml
<!-- Jackson ya viene gestionado por spring-boot-starter-web -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

⚠️ **Riesgo técnico:**
Las dependencias `jackson-databind` y `jackson-datatype-jsr310` ya están gestionadas por `spring-boot-starter-web` a través del BOM de Spring Boot. La declaración explícita crea ambigüedad de versión y puede causar conflictos si se especifica una versión diferente.

📋 **Referencia:** CWE-1104 (Use of Unmaintained Third Party Components)

✅ **Código refactorizado:**
```xml
<!-- Eliminar las dependencias explícitas de Jackson.
     spring-boot-starter-web ya las incluye con versiones gestionadas por el BOM. -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

📋 **Validación:**
1. `mvn dependency:tree | grep jackson` debe mostrar las versiones gestionadas por Spring Boot
2. La aplicación debe compilar y funcionar igual sin las dependencias explícitas

---

### HALLAZGO #14 — SEVERIDAD: LOW — CWE-215 — CVSS 3.1

**Superficie de ataque:** A09 Security Logging and Monitoring Failures

📁 **Archivo:** `.gitignore`
📍 **Línea(s):** 1-40

🔍 **Código vulnerable:**
```gitignore
# Ausencia de patrones críticos de seguridad:
# - .env / .env.* no están excluidos (excepto .env*.local del frontend)
# - *.pem, *.key, *.p12 no están excluidos
```

⚠️ **Riesgo técnico:**
El `.gitignore` actual no protege contra el commit accidental de archivos de secretos. Si un desarrollador crea un archivo `.env` con credenciales reales, puede ser commiteado accidentalmente. Los certificados y claves privadas tampoco están protegidos.

📋 **Referencia:** CWE-215 (Insertion of Sensitive Information Into Debugging Code)

✅ **Código refactorizado (agregar al `.gitignore` existente):**
```gitignore
# ── Security: secrets and credentials ──────────────────────────
.env
.env.*
!.env.example
*.pem
*.key
*.p12
*.pfx
*.jks
*.keystore
*.cert
*.crt

# ── Security: OWASP dependency-check ─────────────────────────
dependency-check-suppressions.xml

# ── Security: Vault tokens ───────────────────────────────────
.vault-token
.vault/
```

📋 **Validación:**
1. `echo "SECRET=test" > .env && git add .env` debe mostrar error de .gitignore
2. `echo "test" > test.pem && git add test.pem` debe mostrar error de .gitignore

---

## INYECCIÓN SQL/HQL — RESULTADO DE ANÁLISIS

**Resultado:** No se encontraron vectores de inyección SQL.

Las dos consultas `@Query` en `CriticalOutboxRepository.java:13-25` utilizan JPQL parametrizado con `:param` y `@Param` correctamente:

```java
@Query("""
        select e from CriticalOutboxEntity e
        where e.delivered = false and e.nextAttemptAt <= :now
        order by e.nextAttemptAt asc
        """)
List<CriticalOutboxEntity> findDue(@Param("now") Instant now, Pageable pageable);
```

- No se encontró uso de `EntityManager.createNativeQuery()`
- No se encontró uso de `CriteriaBuilder.literal()`
- No se encontró concatenación de strings en queries
- Las entidades JPA utilizan `jakarta.persistence.*` correctamente (sin restos de `javax.persistence.*`)

---

## DESERIALIZACIÓN JACKSON — RESULTADO DE ANÁLISIS

**Resultado:** No se encontraron vulnerabilidades críticas de deserialización.

- No se encontró uso de `enableDefaultTyping()` ni `activateDefaultTyping()`
- No se encontró uso de `@JsonTypeInfo` ni `@JsonSubTypes`
- No hay deserialización polimórfica en el proyecto
- El `ObjectMapper` en `JacksonConfig.java` es seguro pero carece de hardening explícito (Hallazgo #9)

---

## EXPOSICIÓN DE ENDPOINTS ACTUATOR — RESULTADO DE ANÁLISIS

**Resultado:** No aplica — `spring-boot-starter-actuator` no está presente en el proyecto.

| Aspecto | Estado |
|---------|--------|
| Dependencia actuator | No presente |
| Configuración management.* | No existe |
| Riesgo de exposición | N/A (superficie inexistente) |
| Recomendación | Agregar actuator con configuración restrictiva para health checks |

---

## TABLA RESUMEN EJECUTIVO

| # | Hallazgo | Superficie | CWE | Severidad | CVSS v3.1 | Estado |
|---|----------|-----------|-----|-----------|-----------|--------|
| 1 | Sin autenticación/autorización en endpoints API | A01 | CWE-306 | CRITICAL | 9.8 | Acción inmediata |
| 2 | Contraseña Oracle SYS hardcodeada (`admin123`) | A07 | CWE-798 | CRITICAL | 9.1 | Acción inmediata |
| 3 | Credenciales DB hardcodeadas en SQL/JSON | A07 | CWE-798 | CRITICAL | 8.6 | Acción inmediata |
| 4 | SMTP password con default `changeme` | A07 | CWE-798 | HIGH | 7.5 | Acción inmediata |
| 5 | CORS wildcard `*` en todos los endpoints | A05 | CWE-942 | HIGH | 7.3 | Acción inmediata |
| 6 | SMTP sin autenticación ni TLS | A02 | CWE-319 | HIGH | 7.4 | Acción inmediata |
| 7 | Sin OWASP dependency-check ni maven-enforcer | A06 | CWE-693 | HIGH | 7.0 | Acción inmediata |
| 8 | Deploy a producción salta tests (`-DskipTests`) | A06 | CWE-693 | HIGH | 7.0 | Acción inmediata |
| 9 | Jackson ObjectMapper sin hardening explícito | A08 | CWE-502 | MEDIUM | 5.9 | Recomendación |
| 10 | Privilegios Oracle excesivos (`SELECT ANY TABLE`) | A01 | CWE-250 | MEDIUM | 6.5 | Recomendación |
| 11 | Puertos internos expuestos al host | A01 | CWE-200 | MEDIUM | 5.3 | Recomendación |
| 12 | Kafka sin TLS ni SASL | A02 | CWE-327 | MEDIUM | 5.3 | Recomendación |
| 13 | Dependencias Jackson redundantes | A06 | CWE-1104 | LOW | 3.7 | Informativo |
| 14 | .gitignore incompleto para secretos | A09 | CWE-215 | LOW | 3.1 | Informativo |

---

## PLAN DE REMEDIACIÓN PRIORIZADO

### FASE 1 — INMEDIATA (bloqueantes de release, CVSS ≥ 7.0, ≤ 1 semana)

| Prioridad | Hallazgo | Esfuerzo | Impacto |
|-----------|----------|----------|---------|
| P0 | #1 — Spring Security + autenticación endpoints | 4h | CRITICAL |
| P0 | #2 — Credenciales Oracle a variables de entorno | 30min | CRITICAL |
| P0 | #3 — Credenciales DB a variables de entorno | 1h | CRITICAL |
| P1 | #4 — SMTP password sin default hardcodeado | 15min | HIGH |
| P1 | #5 — CORS restringido a orígenes configurados | 1h | HIGH |
| P1 | #6 — SMTP auth + TLS habilitado | 15min | HIGH |
| P1 | #7 — OWASP dependency-check + maven-enforcer | 2h | HIGH |
| P1 | #8 — Deploy pipeline sin skipTests | 5min | HIGH |

### FASE 2 — CORTO PLAZO (30 días, CVSS 4.0-6.9)

| Prioridad | Hallazgo | Esfuerzo | Impacto |
|-----------|----------|----------|---------|
| P2 | #9 — Jackson hardening explícito | 30min | MEDIUM |
| P2 | #10 — Oracle privilegios mínimos | 1h | MEDIUM |
| P2 | #11 — Puertos Docker restringidos | 30min | MEDIUM |
| P2 | #12 — Kafka TLS/SASL | 4h | MEDIUM |

### FASE 3 — MEJORA CONTINUA (CVSS < 4.0, backlog)

| Prioridad | Hallazgo | Esfuerzo | Impacto |
|-----------|----------|----------|---------|
| P3 | #13 — Eliminar dependencias Jackson redundantes | 10min | LOW |
| P3 | #14 — .gitignore para secretos | 5min | LOW |
| P3 | Agregar Spring Boot Actuator con hardening | 2h | Defensa en profundidad |
| P3 | Migrar secretos a HashiCorp Vault | 8h | Defensa en profundidad |
| P3 | Integrar SemGrep + SpotBugs/FindSecBugs en CI | 4h | Defensa en profundidad |

---

## COMANDOS MAVEN POST-REMEDIACIÓN

```bash
# Verificación completa con OWASP dependency-check
mvn clean verify -Powasp

# Solo dependency-check
mvn dependency-check:check

# Verificación con enforcer (incluido en verify por defecto)
mvn clean verify

# Reporte de dependency-check generado en:
# target/dependency-check-report.html

# Verificar que no hay dependencias con CVEs críticos
mvn org.owasp:dependency-check-maven:10.0.3:check \
    -DfailBuildOnCVSS=7.0 \
    -Dformat=HTML
```

---

## RECOMENDACIONES DE INTEGRACIÓN CI/CD

**Pipeline de seguridad sugerido para `.github/workflows/ci.yml`:**

```yaml
- name: OWASP Dependency Check
  run: mvn --batch-mode org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7.0

- name: SpotBugs with FindSecBugs
  uses: spotbugs/spotbugs-github-action@v1
  with:
    arguments: '-pluginList com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0'

- name: SemGrep SAST
  uses: returntocorp/semgrep-action@v1
  with:
    config: >-
      p/owasp-top-ten
      p/java
      p/security-audit
```

---

*Informe generado el 2026-03-31. Próxima auditoría recomendada: Q3 2026 o tras cambios significativos en dependencias/arquitectura.*
