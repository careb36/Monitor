# Security Hardening Readiness Report

Date: 2026-04-02
Scope: Remaining findings from SECURITY-AUDIT.md (#8 to #14)
Target branch: develop

## Executive Summary
Security hardening phases for findings #8 to #14 were implemented across CI/CD, backend configuration, Docker runtime exposure, Kafka secure-mode rollout, and operational controls.

Readiness conclusion:
- Develop environment: READY with compensating controls
- Production promotion: CONDITIONALLY READY, requires secure-mode enforcement decision for Kafka (mandatory SASL_SSL cutover)

## Findings Status Matrix

| Finding | Severity | Status | Evidence |
|---|---|---|---|
| #8 (deploy pipeline skips tests) | HIGH | Closed | `.github/workflows/deploy.yml` uses `mvn --batch-mode clean verify -Powasp` |
| #9 (Jackson hardening) | MEDIUM | Closed | `src/main/java/com/monitor/config/JacksonConfig.java` enforces unknown-property fail and disables default typing |
| #10 (Oracle privilege/user hardcoding) | MEDIUM | Mitigated | `docker/oracle/init.sql` parameterized user/schema references, reduced hardcoded account coupling |
| #11 (Docker port exposure) | MEDIUM | Closed | `docker-compose.yml` binds exposed ports to localhost by default (`127.0.0.1`) |
| #12 (Kafka plaintext transport) | MEDIUM | Mitigated with rollout controls | `docker-compose.secure.yml` + secure scripts + preflight + smoke + acceptance checklist |
| #13 (dependency hygiene) | LOW | Closed | `pom.xml` duplicate security dependency removed |
| #14 (secret file protection) | LOW | Closed | `.gitignore` includes secret patterns and constrained exceptions for tracked placeholders |

## Implemented Controls (Phase Trace)

### CI/CD and dependency gates
- Production deploy workflow executes full verification with OWASP profile.
- Dependency check and enforcer are integrated in Maven build lifecycle.

### Serialization hardening
- ObjectMapper hardened with:
  - `FAIL_ON_UNKNOWN_PROPERTIES`
  - `deactivateDefaultTyping()`
  - restricted visibility configuration

### Database/script hardening
- Oracle init script no longer depends on hardcoded schema/user references for grants and DDL context.
- Current schema is parameterized via `&MONITOR_APP_USER`.

### Runtime exposure reduction
- Container ports default-bind to loopback host, reducing accidental external exposure in local/proxy-hosted environments.

### Kafka secure transport rollout controls
Implemented artifacts:
- `docker-compose.secure.yml`
- `scripts/kafka-generate-secrets.sh`
- `scripts/kafka-bootstrap-scram-users.sh`
- `scripts/kafka-secure-preflight.sh`
- `scripts/kafka-enable-secure-mode.sh`
- `scripts/kafka-secure-smoke.sh`
- `docs/operations/kafka-sasl-ssl-quickstart.md`
- `docs/operations/kafka-sasl-ssl-troubleshooting.md`
- `docs/operations/kafka-sasl-ssl-acceptance-checklist.md`

These controls establish an auditable path to run Kafka in SASL_SSL and validate it before promotion.

## Residual Risk

### Finding #12 residual risk
Base compose remains PLAINTEXT-compatible by default to preserve backward compatibility and low-risk local onboarding. This is acceptable for staged rollout but should not be the final production posture.

Residual risk rating: Medium (operationally controlled, not yet hard-enforced by default)

## Release Decision

For develop:
- APPROVED

For production:
- APPROVED WITH CONDITION

Condition to fully close finding #12 in production:
1. Enforce secure overlay in production deployment path.
2. Remove PLAINTEXT listener usage from production runtime configuration.
3. Attach smoke/preflight evidence artifacts in release PR.

## Evidence Checklist for Release PR
- Output of `./scripts/kafka-secure-preflight.sh`
- Output of `./scripts/kafka-secure-smoke.sh`
- `docker compose ps` with secure overlay
- Last 200 lines of logs for `kafka`, `kafka-connect`, `backend`
- Confirmation that deploy pipeline uses verify + OWASP profile
