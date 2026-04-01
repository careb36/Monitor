# AGENTS.md

## English

### Purpose

This file gives AI coding agents a concise, implementation-aligned operating model for `Monitor`.
Use it as a guardrail document before changing runtime behavior, tests, SSE contracts, or delivery configuration.

### Quick Start

- MUST run backend checks with `mvn --batch-mode clean verify`.
- MUST run the frontend locally with `cd frontend && npm install && npm run dev`.
- MUST preserve the local API rewrite in `frontend/next.config.js` (`/api/* -> http://localhost:8080/api/*`).

### Core Architecture

- `src/main/java/com/monitor/service/EventBus.java` is the high-performance SSE fan-out hub using **Java 21 Virtual Threads**.
- `src/main/java/com/monitor/controller/SseController.java` exposes `GET /api/events/stream`.
- Producers are:
  - `KafkaConsumerService` for CDC / Debezium events
  - `PollingService` for scheduled infrastructure checks
- `EmailService` sends asynchronous notifications only for CRITICAL conditions.

### Non-Negotiable Contracts

- MUST keep SSE event naming aligned to `event.getType().name().toLowerCase()`.
- MUST keep frontend listeners aligned with backend event names in `frontend/src/hooks/useMonitor.ts`.
- MUST use **Virtual Threads** for parallel broadcasting to ensure O(1) scalability for 10k+ clients.
- MUST update all relevant locations when adding a new event type:
  - `src/main/java/com/monitor/model/EventType.java`
  - `frontend/src/lib/types.ts`
  - `frontend/src/hooks/useMonitor.ts`
- MUST keep CRITICAL flow end-to-end: dashboard visibility via SSE plus `EmailService.sendCriticalAlert(...)`.
- MUST keep configuration in `src/main/resources/application.yml`; do not hardcode topics, recipients, or polling targets.

### Testing Guidance

- MUST prefer service-level tests in `src/test/java/com/monitor/service/`.
- MUST use plain JUnit 5 + Mockito for service tests; avoid loading the Spring context unless truly necessary.
- MUST use **Awaitility** or similar asynchronous verification for `EventBus` tests, as broadcasting is now non-blocking.
- SHOULD preserve `PollingService.simulatePing` as package-private for anonymous-subclass test overrides.
- SHOULD use `ReflectionTestUtils.setField(...)` only where typed constructor/configuration alternatives do not already exist.

### Runtime and Delivery Notes

- `src/main/java/com/monitor/App.java` enables both scheduling and async execution.
- `EventBus` uses `ConcurrentHashMap.newKeySet()` and `Executors.newVirtualThreadPerTaskExecutor()` for fan-out.
- `EventBus` sends `:connected` and periodic `:heartbeat` comments on the SSE stream.
- Kafka parsing expects a Debezium payload with `payload.after` and `op == "c"`.
- `.github/workflows/lint.yml` enforces Conventional Commits and branch naming.
- CI quality gates run before deploy; `main` deploy builds with `-DskipTests`.

### Definition of Done

- MUST preserve the backend/frontend SSE contract.
- MUST preserve CRITICAL alert duplication to both SSE and email.
- MUST keep tests aligned with behavioral changes.
- MUST pass `mvn --batch-mode clean verify` before considering a change complete.

---

## Español

### Propósito

Este archivo ofrece a los agentes de IA un modelo operativo breve y alineado con la implementación actual de `Monitor`.
Úsalo como documento de guardrails antes de cambiar comportamiento de runtime, pruebas, contratos SSE o configuración de entrega.

### Inicio Rápido

- MUST ejecutar validaciones de backend con `mvn --batch-mode clean verify`.
- MUST ejecutar el frontend localmente con `cd frontend && npm install && npm run dev`.
- MUST preservar el rewrite local de API en `frontend/next.config.js` (`/api/* -> http://localhost:8080/api/*`).

### Arquitectura Central

- `src/main/java/com/monitor/service/EventBus.java` es el hub SSE de alto rendimiento mediante **Hilos Virtuales de Java 21**.
- `src/main/java/com/monitor/controller/SseController.java` expone `GET /api/events/stream`.
- Los productores son:
  - `KafkaConsumerService` para eventos CDC / Debezium
  - `PollingService` para chequeos programados de infraestructura
- `EmailService` envía notificaciones asíncronas solo para condiciones CRITICAL.

### Contratos No Negociables

- MUST mantener la nomenclatura SSE alineada a `event.getType().name().toLowerCase()`.
- MUST mantener alineados los listeners frontend con los nombres emitidos por backend en `frontend/src/hooks/useMonitor.ts`.
- MUST utilizar **Hilos Virtuales** para el despacho paralelo, asegurando escalabilidad O(1) para 10.000+ clientes.
- MUST actualizar todas las ubicaciones relevantes cuando se agregue un nuevo tipo de evento:
  - `src/main/java/com/monitor/model/EventType.java`
  - `frontend/src/lib/types.ts`
  - `frontend/src/hooks/useMonitor.ts`
- MUST mantener el flujo CRITICAL de extremo a extremo: visibilidad en dashboard por SSE más `EmailService.sendCriticalAlert(...)`.
- MUST mantener la configuración en `src/main/resources/application.yml`; no hardcodear topics, destinatarios ni targets de polling.

### Guía de Testing

- MUST preferir pruebas de servicios en `src/test/java/com/monitor/service/`.
- MUST usar JUnit 5 + Mockito sin cargar el contexto Spring para pruebas de servicios, salvo necesidad real.
- MUST utilizar **Awaitility** o verificación asíncrona similar para los tests de `EventBus`, ya que el despacho ahora es no bloqueante.
- SHOULD preservar `PollingService.simulatePing` como package-private para overrides con subclases anónimas en tests.
- SHOULD usar `ReflectionTestUtils.setField(...)` solo cuando no exista ya una alternativa mejor basada en constructor o configuración tipada.

### Notas de Runtime y Entrega

- `src/main/java/com/monitor/App.java` habilita scheduling y ejecución async.
- `EventBus` utiliza `ConcurrentHashMap.newKeySet()` y `Executors.newVirtualThreadPerTaskExecutor()` para el fan-out.
- `EventBus` envía comentarios `:connected` y `:heartbeat` periódicos en el stream SSE.
- El parseo Kafka espera un payload Debezium con `payload.after` y `op == "c"`.
- `.github/workflows/lint.yml` exige Conventional Commits y nomenclatura de ramas.
- Los quality gates de CI se ejecutan antes del deploy; el deploy de `main` compila con `-DskipTests`.

### Definición de Terminado

- MUST preservar el contrato SSE entre backend y frontend.
- MUST preservar la duplicación de alertas CRITICAL tanto a SSE como a email.
- MUST mantener pruebas alineadas con cambios de comportamiento.
- MUST pasar `mvn --batch-mode clean verify` antes de considerar completo un cambio.
