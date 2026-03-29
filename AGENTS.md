# AGENTS.md

## 1) Quick Start (run this first)
- MUST run backend checks with `mvn --batch-mode clean verify` (same gate as `.github/workflows/ci.yml` and `.github/workflows/release.yml`).
- MUST run frontend locally with `cd frontend && npm install && npm run dev`.
- MUST keep the Next rewrite in `frontend/next.config.js` for local SSE (`/api/*` -> `http://localhost:8080/api/*`).

## 2) Core Event Pipeline (read before changing behavior)
- `src/main/java/com/monitor/service/EventBus.java` is the in-memory fan-out hub (`CopyOnWriteArrayList<SseEmitter>`).
- `src/main/java/com/monitor/controller/SseController.java` exposes `GET /api/events/stream` and registers emitters.
- Producers are `src/main/java/com/monitor/service/KafkaConsumerService.java` (CDC) and `src/main/java/com/monitor/service/PollingService.java` (scheduled infrastructure checks).
- `src/main/java/com/monitor/service/EmailService.java` sends async email only when producers set severity to CRITICAL.

## 3) Contract Rules You MUST Keep
- MUST keep SSE naming contract: backend emits `event.getType().name().toLowerCase()` (`EventBus.publish`), frontend listens in `frontend/src/hooks/useMonitor.ts` (`'infrastructure'`, `'data'`).
- MUST update all three places when adding event types: `src/main/java/com/monitor/model/EventType.java`, `frontend/src/lib/types.ts`, and listeners in `frontend/src/hooks/useMonitor.ts`.
- MUST keep CRITICAL flow end-to-end: publish to SSE and call `EmailService.sendCriticalAlert(...)` (see `KafkaConsumerService.consume` and `PollingService.check`).
- MUST keep configuration in `src/main/resources/application.yml` (`monitor.kafka.*`, `monitor.polling.*`, `monitor.mail.*`) with no hardcoded values.

## 4) Project-Specific Testing Patterns
- MUST prefer service tests with JUnit 5 + Mockito in `src/test/java/com/monitor/service/` (no Spring context boot).
- SHOULD use `ReflectionTestUtils.setField(...)` for `@Value` fields (see `EmailServiceTest`, `PollingServiceTest`).
- SHOULD preserve `PollingService.simulatePing` as package-private for anonymous-subclass overrides in tests.

## 5) Runtime and Integration Notes
- `src/main/java/com/monitor/App.java` enables `@EnableScheduling` and `@EnableAsync`; polling/mail behavior changes depend on this.
- `src/main/java/com/monitor/config/CorsConfig.java` is permissive (`allowedOriginPatterns("*")`), so browser/CORS issues may be environment-specific.
- Kafka consumer topic is configured by `monitor.kafka.topic.log-traza`; parsing expects a Debezium payload with `payload.after` and `op == "c"`.

## 6) CI and Branch Guardrails
- MUST follow Conventional Commits in PR titles; enforced in `.github/workflows/lint.yml`.
- MUST use allowed branch prefixes (`feature/`, `bugfix/`, `release/`, `hotfix/`, plus listed automation prefixes) per `.github/workflows/lint.yml`.
- `main` deploy in `.github/workflows/deploy.yml` uses `-DskipTests`; quality gates happen earlier in CI/release workflows.

## 7) Definition of Done
- MUST keep the backend/frontend SSE contract (`event.getType().name().toLowerCase()` and expected listeners in `frontend/src/hooks/useMonitor.ts`).
- MUST guarantee CRITICAL flow end-to-end: event visible via SSE and call to `EmailService.sendCriticalAlert(...)`.
- MUST keep all operational configuration in `src/main/resources/application.yml` (`monitor.kafka.*`, `monitor.polling.*`, `monitor.mail.*`) with no hardcoding.
- MUST keep or update service unit tests in `src/test/java/com/monitor/service/` for behavior changes.
- MUST pass `mvn --batch-mode clean verify` before closing the change.

