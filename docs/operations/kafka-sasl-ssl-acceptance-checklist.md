# Kafka SASL/SSL Acceptance Checklist

Use this checklist to declare Hallazgo #12 operationally closed.

## Preconditions
- [ ] `.env` contains secure Kafka credentials
- [ ] TLS artifacts exist under `docker/kafka/secrets/`
- [ ] Preflight passes: `./scripts/kafka-secure-preflight.sh`

## Activation
- [ ] Secure mode applied: `./scripts/kafka-enable-secure-mode.sh`
- [ ] Overlay is active: `docker compose -f docker-compose.yml -f docker-compose.secure.yml ps`

## Smoke validation
- [ ] `./scripts/kafka-secure-smoke.sh` exits with code 0
- [ ] `kafka`, `kafka-connect`, `backend` are running
- [ ] Backend env has `KAFKA_SECURITY_PROTOCOL=SASL_SSL`
- [ ] Connect env has `CONNECT_SECURITY_PROTOCOL=SASL_SSL`
- [ ] Recent logs have no TLS/SASL handshake/auth failures

## Functional validation
- [ ] Debezium connector stays healthy
- [ ] CDC events still arrive to backend pipeline
- [ ] No regression on SSE event delivery

## Rollback validated
- [ ] Plaintext fallback works: `docker compose -f docker-compose.yml up -d`
- [ ] Services recover after rollback

## Evidence to attach in PR/issue
- [ ] `docker compose ps` output (secure mode)
- [ ] `./scripts/kafka-secure-preflight.sh` output
- [ ] `./scripts/kafka-secure-smoke.sh` output
- [ ] relevant logs for kafka/connect/backend

## Phase Closure Snapshot (2026-04-02)
- [x] PR #45 merged into `develop` with secure bootstrap stabilization
- [x] PR #44 merged (`develop` -> `main`) after evidence comment
- [x] Secure smoke executed with PASS result
- [x] Kafka secure stack reached healthy state (`kafka`, `backend`, `kafka-connect` running)
