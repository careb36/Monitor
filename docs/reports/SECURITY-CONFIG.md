# Security Configuration Guide – Monitor Dashboard

## Overview

This document describes all security controls implemented in the Monitor Dashboard backend, aligned with OWASP Top 10 (2021) and CWE/SANS Top 25 (2023).

## Table of Contents

1. [Authentication & Authorization](#1-authentication--authorization)
2. [Security Headers](#2-security-headers)
3. [CORS Configuration](#3-cors-configuration)
4. [Secret Management](#4-secret-management)
5. [Rate Limiting](#5-rate-limiting)
6. [Dependency Security](#6-dependency-security)
7. [Docker Hardening](#7-docker-hardening)
8. [Audit Logging](#8-audit-logging)
9. [CI/CD Pipeline Security](#9-cicd-pipeline-security)

---

## 1. Authentication & Authorization

**OWASP A07:2021 – Identification and Authentication Failures**

### Implementation

- **HTTP Basic Auth** on all `/api/events/stream` and `/api/admin/**` endpoints
- **BCrypt** password hashing with 12 rounds (NIST SP 800-63B compliant)
- **Stateless sessions** – no server-side session state
- **Role-based access**: `ROLE_MONITOR_USER` for SSE, `ROLE_MONITOR_ADMIN` for admin

### Configuration

```yaml
# Environment variables (NEVER in application.yml directly)
MONITOR_USER=monitor
MONITOR_PASSWORD=<generate with: openssl rand -base64 32>
```

### Code

- `SecurityConfig.java` – filter chains per profile
- Dev profile: relaxed (any authenticated user)
- Staging/Prod profile: strict (role-based)

---

## 2. Security Headers

**OWASP A05:2021 – Security Misconfiguration**

### Headers Applied (non-dev profiles)

| Header | Value | Purpose |
|--------|-------|---------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Force HTTPS (1 year) |
| `X-Content-Type-Options` | `nosniff` | Prevent MIME type sniffing (CWE-16) |
| `X-Frame-Options` | `DENY` | Prevent clickjacking (CWE-1021) |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS filter |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limit referrer leakage |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'; base-uri 'none'` | Strict CSP for API |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=()` | Disable browser features |

### Code

- `SecurityConfig.java:79-96` – headers configured in `prodFilterChain`

---

## 3. CORS Configuration

**OWASP A05:2021 – Security Misconfiguration**

### Policy

- Origins loaded from `monitor.cors.allowed-origins` property
- Only `GET` and `OPTIONS` methods allowed (SSE is read-only)
- Specific headers: `Authorization`, `Accept`, `Cache-Control`, `Last-Event-ID`
- Credentials disabled (API uses HTTP Basic, not cookies)
- Preflight cache: 3600 seconds
- Blocked origin attempts logged

### Configuration

```yaml
monitor:
  cors:
    allowed-origins: http://localhost:3000  # Dev
    # In production: https://dashboard.yourdomain.com
```

### Code

- `CorsConfig.java` – origin whitelist from config, logging

---

## 4. Secret Management

**OWASP A07:2021 – CWE-798 (Use of Hard-coded Credentials)**

### Principles

1. **No secrets in source code** – all via environment variables
2. **No defaults with weak values** – `changeme`, `password`, `123` cause startup failure
3. **Fail-fast** – app won't start if required secrets are missing
4. **`.gitignore`** protects against accidental commits

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ORACLE_PASSWORD` | Oracle SYS password | `openssl rand -base64 32` |
| `MONITOR_APP_PASSWORD` | Oracle app user password | `openssl rand -base64 32` |
| `MONITOR_PASSWORD` | HTTP Basic auth password | `openssl rand -base64 32` |
| `MAIL_PASSWORD` | SMTP credentials | From mail admin |

### Pre-flight Validation

```bash
source .env
./scripts/secrets-validator.sh
```

### Files

- `.env.example` – template with documentation
- `.env` – actual values (NEVER committed)
- `scripts/secrets-validator.sh` – pre-flight check

---

## 5. Rate Limiting

**OWASP A07:2021 – CWE-307 (Improper Restriction of Excessive Authentication Attempts)**

### Implementation

- **Bucket4j** in-memory token bucket per client IP
- Applied to `/api/events/**` and `/api/admin/**` endpoints only
- Configurable capacity and refill rate per profile

### Configuration

```yaml
monitor:
  rate-limit:
    enabled: true
    capacity: 20              # Dev: 20 req/min
    refill-tokens: 20
    refill-duration-seconds: 60
```

Prod profile: 10 req/min (stricter)

### Response Headers

- `X-RateLimit-Remaining` – tokens left
- `Retry-After` – seconds to wait when limited (HTTP 429)

### Code

- `RateLimitingFilter.java` – servlet filter with Bucket4j

---

## 6. Dependency Security

**OWASP A06:2021 – Vulnerable and Outdated Components**

### Tools

| Tool | Purpose | Configuration |
|------|---------|---------------|
| OWASP Dependency-Check | CVE scanning | `failBuildOnCVSS=7.0`, HTML report |
| Maven Enforcer | Environment validation | Java 21, Maven 3.9+, no SNAPSHOTs |
| SpotBugs + FindSecBugs | SAST scanning | Max effort, Medium threshold |

### Commands

```bash
# Full security scan
mvn clean verify -Powasp

# Individual tools
mvn dependency-check:check     # CVE report: target/dependency-check-report.html
mvn spotbugs:check             # SAST report
mvn enforcer:enforce           # Environment validation
```

### Suppressions

- `dependency-check-suppressions.xml` – documented false positives
- Review quarterly or after dependency upgrades

---

## 7. Docker Hardening

**OWASP A05:2021 – Security Misconfiguration**

### Network Isolation

- All services on `monitor-net` bridge network with defined subnet (172.28.0.0/16)
- Internal service ports NOT exposed to host by default
- Conditional port exposure via env vars (`ORACLE_PORT`, `KAFKA_PORT`, `CONNECT_PORT`)

### Credentials

- All passwords via environment variables (no hardcoding)
- `docker compose up` fails if required env vars are missing (`:?` syntax)

### Healthchecks

All services have healthchecks configured for proper dependency ordering:
- Oracle: `healthcheck.sh`
- Zookeeper: `echo ruok | nc localhost 2181`
- Kafka: `kafka-topics --list`
- Kafka Connect: `curl -f http://localhost:8083/connectors`

---

## 8. Audit Logging

**OWASP A09:2021 – Security Logging and Monitoring Failures**

### Implementation

- `SecurityAuditAspect.java` – AOP-based logging on all controller methods
- Separate `SECURITY_AUDIT` logger for SIEM integration
- Logs: endpoint access, execution time, errors, rate limit events

### Logged Events

| Event | Log Level | Details |
|-------|-----------|---------|
| SSE connection | INFO | Client IP, timestamp |
| Slow endpoint (>5s) | WARN | Method, duration |
| Rate limit violation | WARN | IP, path, method |
| Endpoint error | ERROR | Method, duration, error |

### Log Levels per Profile

| Profile | Level |
|---------|-------|
| dev | DEBUG |
| staging | INFO |
| prod | WARN |

---

## 9. CI/CD Pipeline Security

### GitHub Actions Security Steps

1. **Build & Test** – `mvn clean verify` (no `-DskipTests` in production)
2. **Dependency Check** – OWASP dependency-check on every build
3. **SAST** – SpotBugs with FindSecBugs plugin
4. **Deploy** – only from `main` branch after all checks pass

### Commands

```bash
# CI: Full verification
mvn --batch-mode clean verify

# CD: Production build with OWASP scan
mvn --batch-mode clean verify -Powasp
```

---

## Security Contacts

For security issues, contact the operations team:
- Email: ops-team@example.com
- On-call: oncall@example.com

---

*Last updated: 2026-03-31*
