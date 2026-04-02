# Monitor Documentation

**Architecture, Decisions, and Operations Hub / Hub de Arquitectura, Decisiones y Operaciones**

[English](#english) | [Español](#español)

---

<a id="english"></a>
## English

### Executive Summary

This documentation set is the primary reference library for understanding, operating, and evolving `Monitor`.
It is designed to serve three audiences:

- executive and architecture stakeholders who need high-level system views
- engineers who need implementation-aligned diagrams and decision records
- operators and support engineers who need validation and troubleshooting guidance

### Documentation Map

#### C4 Diagrams

- `docs/diagrams/10-c4-system-context.puml` - Level 1 system context
- `docs/diagrams/11-c4-container-view.puml` - Level 2 container view
- `docs/diagrams/12-c4-backend-component-view.puml` - Level 3 backend component view

#### Runtime and Technical Diagrams

- `docs/diagrams/01-architecture-overview.puml` - executive technical overview
- `docs/diagrams/02-sse-runtime-sequence.puml` - SSE runtime lifecycle
- `docs/diagrams/03-critical-alert-sequence.puml` - CRITICAL event flow
- `docs/diagrams/04-local-deployment.puml` - local deployment topology
- `docs/diagrams/05-backend-class-diagram.puml` - backend class structure

#### Architecture Decision Records (ADRs)

- `docs/adr/README.md` - ADR index and conventions
- `docs/adr/0000-adr-template.md`
- `docs/adr/0001-use-sse-eventbus-fanout.md`
- `docs/adr/0002-duplicate-critical-events-to-sse-and-email.md`
- `docs/adr/0003-refactor-eventbus-virtual-threads-and-concurrenthashmap.md`

#### Operations and Runbooks

- `docs/operations/README.md` - operations index
- `docs/operations/monitor-operations-runbook.md` - operational runbook

#### Security Reports

- `docs/reports/SECURITY-AUDIT.md` - baseline security assessment and findings catalog
- `docs/reports/SECURITY-HARDENING-READINESS.md` - current closure/readiness status for findings #8-#14

### Recommended Reading Paths

#### For executives or architects
1. `docs/diagrams/10-c4-system-context.puml`
2. `docs/diagrams/11-c4-container-view.puml`
3. `docs/diagrams/01-architecture-overview.puml`

#### For backend engineers
1. `docs/diagrams/12-c4-backend-component-view.puml`
2. `docs/diagrams/05-backend-class-diagram.puml`
3. `docs/adr/0001-use-sse-eventbus-fanout.md`
4. `docs/adr/0002-duplicate-critical-events-to-sse-and-email.md`

#### For operators or support engineers
1. `docs/operations/monitor-operations-runbook.md`
2. `docs/diagrams/02-sse-runtime-sequence.puml`
3. `docs/diagrams/03-critical-alert-sequence.puml`
4. `docs/diagrams/04-local-deployment.puml`

### Source Alignment

This documentation is grounded in the current implementation, especially:

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `src/main/java/com/monitor/config/JacksonConfig.java`
- `src/main/java/com/monitor/config/MonitorMailProperties.java`
- `src/main/java/com/monitor/config/MonitorPollingTargetsProperties.java`
- `frontend/src/hooks/useMonitor.ts`
- `frontend/src/app/page.tsx`
- `frontend/next.config.js`
- `docker-compose.yml`

### Rendering Notes

All PlantUML diagrams share a common include file:

- `docs/diagrams/_theme.puml`

Open any `.puml` file directly in your IDE plugin to render it.

---

<a id="español"></a>
## Espanol

### Resumen Ejecutivo

Este set documental es la biblioteca principal para entender, operar y evolucionar `Monitor`.
Esta dise�ado para tres audiencias:

- stakeholders ejecutivos y de arquitectura que necesitan vistas de alto nivel
- ingenieros que necesitan diagramas y decisiones alineadas a la implementacion
- operadores y soporte que necesitan guias de validacion y troubleshooting

### Mapa Documental

#### Diagramas C4

- `docs/diagrams/10-c4-system-context.puml` - contexto del sistema nivel 1
- `docs/diagrams/11-c4-container-view.puml` - vista de contenedores nivel 2
- `docs/diagrams/12-c4-backend-component-view.puml` - vista de componentes backend nivel 3

#### Diagramas de Runtime y Tecnica

- `docs/diagrams/01-architecture-overview.puml` - vista tecnica ejecutiva
- `docs/diagrams/02-sse-runtime-sequence.puml` - ciclo de vida SSE
- `docs/diagrams/03-critical-alert-sequence.puml` - flujo de eventos CRITICAL
- `docs/diagrams/04-local-deployment.puml` - topologia de despliegue local
- `docs/diagrams/05-backend-class-diagram.puml` - estructura de clases backend

#### Architecture Decision Records (ADRs)

- `docs/adr/README.md` - indice y convenciones ADR
- `docs/adr/0000-adr-template.md`
- `docs/adr/0001-use-sse-eventbus-fanout.md`
- `docs/adr/0002-duplicate-critical-events-to-sse-and-email.md`
- `docs/adr/0003-refactor-eventbus-virtual-threads-and-concurrenthashmap.md`

#### Operaciones y Runbooks

- `docs/operations/README.md` - indice operativo
- `docs/operations/monitor-operations-runbook.md` - runbook operativo

#### Reportes de Seguridad

- `docs/reports/SECURITY-AUDIT.md` - evaluacion base de seguridad y catalogo de hallazgos
- `docs/reports/SECURITY-HARDENING-READINESS.md` - estado actual de cierre/readiness para hallazgos #8-#14

### Rutas de Lectura Recomendadas

#### Para ejecutivos o arquitectos
1. `docs/diagrams/10-c4-system-context.puml`
2. `docs/diagrams/11-c4-container-view.puml`
3. `docs/diagrams/01-architecture-overview.puml`

#### Para ingenieros backend
1. `docs/diagrams/12-c4-backend-component-view.puml`
2. `docs/diagrams/05-backend-class-diagram.puml`
3. `docs/adr/0001-use-sse-eventbus-fanout.md`
4. `docs/adr/0002-duplicate-critical-events-to-sse-and-email.md`

#### Para operadores o soporte
1. `docs/operations/monitor-operations-runbook.md`
2. `docs/diagrams/02-sse-runtime-sequence.puml`
3. `docs/diagrams/03-critical-alert-sequence.puml`
4. `docs/diagrams/04-local-deployment.puml`

### Alineacion con el Codigo

Esta documentacion se apoya en la implementacion actual, especialmente en:

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `src/main/java/com/monitor/config/JacksonConfig.java`
- `src/main/java/com/monitor/config/MonitorMailProperties.java`
- `src/main/java/com/monitor/config/MonitorPollingTargetsProperties.java`
- `frontend/src/hooks/useMonitor.ts`
- `frontend/src/app/page.tsx`
- `frontend/next.config.js`
- `docker-compose.yml`

### Notas de Renderizado

Todos los diagramas PlantUML comparten un include comun:

- `docs/diagrams/_theme.puml`

Abre cualquier archivo `.puml` directamente en tu plugin del IDE para renderizarlo.
