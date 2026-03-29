# Architecture Decision Records (ADRs)

**Architectural Rationale Library / Biblioteca de Racional Arquitectónico**

[English](#english) | [Español](#español)

---

<a id="english"></a>
## English

### Executive Summary

This folder captures durable decisions that explain why the current architecture works the way it does.
It complements diagrams and code by recording the rationale behind critical design choices.

### Purpose

This folder captures durable architectural decisions that are already visible in the implementation.
The goal is to explain **why** key choices were made, not only what the code currently does.

### ADR Index

- `0000-adr-template.md` - reusable template for future decisions
- `0001-use-sse-eventbus-fanout.md` - rationale for SSE fan-out through `EventBus`
- `0002-duplicate-critical-events-to-sse-and-email.md` - rationale for CRITICAL propagation to both dashboard and email

### Conventions

- Use incremental numbering: `0003-...`, `0004-...`, etc.
- Keep statuses explicit: `Proposed`, `Accepted`, `Superseded`, `Deprecated`
- Reference concrete implementation files whenever possible
- Prefer documented implemented decisions over aspirational architecture

### Current Reference Set

The initial ADRs are grounded in:

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`

---

<a id="español"></a>
## Español

### Resumen Ejecutivo

Esta carpeta captura decisiones duraderas que explican por qué la arquitectura actual funciona como lo hace.
Complementa los diagramas y el código al registrar el racional detrás de decisiones de diseño críticas.

### Propósito

Esta carpeta captura decisiones arquitectonicas duraderas que ya son visibles en la implementacion.
El objetivo es explicar **por que** se tomaron ciertas decisiones clave, no solo describir lo que el codigo hace hoy.

### Índice ADR

- `0000-adr-template.md` - plantilla reutilizable para futuras decisiones
- `0001-use-sse-eventbus-fanout.md` - justificacion del fan-out SSE mediante `EventBus`
- `0002-duplicate-critical-events-to-sse-and-email.md` - justificacion de la propagacion CRITICAL a dashboard y correo

### Convenciones

- Usa numeracion incremental: `0003-...`, `0004-...`, etc.
- Manten estados explicitos: `Proposed`, `Accepted`, `Superseded`, `Deprecated`
- Referencia archivos concretos de implementacion siempre que sea posible
- Prefiere documentar decisiones ya implementadas sobre arquitectura aspiracional

### Conjunto Actual de Referencia

Los ADRs iniciales se apoyan en:

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`

