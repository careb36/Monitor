# ADR 0002 - Duplicate CRITICAL events to SSE and email

## English

- **Status:** Accepted
- **Date:** 2026-03-28

### Context

A CRITICAL operational condition must satisfy two needs at the same time:

1. become visible immediately in the live dashboard
2. notify operators asynchronously even if they are not actively watching the UI

This behavior is already implemented in the producer layer rather than hidden inside the transport layer.
Current references include:

- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`

### Decision

When a producer determines that an event is `Severity.CRITICAL`, it must:

- publish the `UnifiedEvent` to `EventBus` for SSE delivery
- invoke `EmailService.sendCriticalAlert(...)` for asynchronous email notification

This is intentionally handled by the producers (`KafkaConsumerService` and `PollingService`) because they own the business interpretation of severity.
`EmailService` is asynchronous and focused only on mail delivery, not on deciding whether something is critical.

### Consequences

#### Positive
- CRITICAL behavior is explicit in the business flow
- dashboard visibility and operator notification stay aligned
- email sending never blocks the main event pipeline because `EmailService` is `@Async`
- different producers can apply domain-specific rules before escalating to CRITICAL

#### Negative / Trade-offs
- producers must remember to trigger both paths when introducing new CRITICAL scenarios
- the duplication is intentional and must be preserved in future refactors
- alerting policy is distributed across producers rather than centralized in one policy engine

### Alternatives Considered

- **Send email inside `EventBus`** - rejected because `EventBus` should remain a transport/fan-out component, not a severity policy layer
- **Central alerting service triggered by every event** - more extensible long term, but unnecessary for the current scope and would add abstraction before it is needed
- **Dashboard-only CRITICAL handling** - rejected because it depends on users actively watching the UI

### Implementation References

- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`

---

## Espanol

- **Estado:** Accepted
- **Fecha:** 2026-03-28

### Contexto

Una condicion operacional CRITICAL debe satisfacer dos necesidades al mismo tiempo:

1. volverse visible inmediatamente en el dashboard en vivo
2. notificar a los operadores de forma asincrona aunque no esten mirando la UI activamente

Este comportamiento ya esta implementado en la capa productora en lugar de ocultarse dentro de la capa de transporte.
Las referencias actuales incluyen:

- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`

### Decision

Cuando un productor determina que un evento es `Severity.CRITICAL`, debe:

- publicar el `UnifiedEvent` en `EventBus` para su entrega por SSE
- invocar `EmailService.sendCriticalAlert(...)` para la notificacion asincrona por correo

Esto se maneja intencionalmente en los productores (`KafkaConsumerService` y `PollingService`) porque ellos poseen la interpretacion de negocio de la severidad.
`EmailService` es asincrono y se enfoca unicamente en la entrega de correo, no en decidir que es critico.

### Consecuencias

#### Positivas
- el comportamiento CRITICAL es explicito dentro del flujo de negocio
- la visibilidad en el dashboard y la notificacion a operadores permanecen alineadas
- el envio de correo nunca bloquea el pipeline principal porque `EmailService` es `@Async`
- distintos productores pueden aplicar reglas de dominio antes de escalar a CRITICAL

#### Negativas / Trade-offs
- los productores deben recordar disparar ambos caminos al introducir nuevos escenarios CRITICAL
- la duplicacion es intencional y debe preservarse en futuros refactors
- la politica de alertamiento queda distribuida entre productores en lugar de centralizarse en un motor de politicas

### Alternativas Consideradas

- **Enviar correo dentro de `EventBus`** - descartado porque `EventBus` debe permanecer como componente de transporte/fan-out, no como capa de politica de severidad
- **Servicio central de alertas disparado por cada evento** - mas extensible a largo plazo, pero innecesario para el alcance actual y anadiria abstraccion antes de ser necesaria
- **Manejo CRITICAL solo en dashboard** - descartado porque depende de que los usuarios esten observando la UI activamente

### Referencias de Implementacion

- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `frontend/src/hooks/useMonitor.ts`
