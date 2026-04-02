# Monitor

**Real-Time Operations Monitoring Platform / Plataforma de Monitoreo Operacional en Tiempo Real**

[English](#english) | [Español](#español)

![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=springboot)
![Next.js](https://img.shields.io/badge/Next.js-15-black?logo=nextdotjs)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.2.0-231F20?logo=apachekafka)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![CI](https://github.com/careb36/Monitor/actions/workflows/ci.yml/badge.svg)

---

<a id="english"></a>
## 🇬🇧 English

### Overview

**Monitor** is a real-time operations monitoring dashboard that streams live events from a Java/Spring Boot backend to a Next.js/React frontend using **Server-Sent Events (SSE)**.

It provides a single pane of glass for two critical data streams:

- **Data events** — changes captured from an Oracle database via Debezium CDC (Change Data Capture) and Apache Kafka.
- **Infrastructure events** — periodic health-check polls of secondary databases and background daemons, with automatic alerting when a target goes DOWN.

Critical events trigger both an instant visual alert in the dashboard (with an audio cue) and an asynchronous email notification to the configured recipients.

### Key Features

| Feature | Details |
|---|---|
| 🔴 **Real-time streaming** | Server-Sent Events push updates instantly to every connected browser tab — no polling required from the client. |
| 📋 **CDC integration** | Debezium captures every `INSERT` into the Oracle `log_traza` table and forwards it to Kafka, which the backend consumes and rebroadcasts. |
| 🩺 **Infrastructure health checks** | Scheduled polls (default: every 30 s) monitor databases and daemons, emitting events only on status transitions to reduce noise. |
| 🔔 **Email alerts** | An async email is sent for every `CRITICAL` event without blocking the event pipeline. |
| 🔊 **Audio cues** | The frontend plays a short beep sequence (Web Audio API) when a `CRITICAL` event arrives. |
| 📊 **Live log dashboard** | The Next.js UI maintains a rolling log of up to 100 events and a per-source infrastructure status board. |
| 🐳 **Docker Compose ready** | The full stack (Oracle XE, Zookeeper, Kafka, Debezium, backend, frontend) with healthchecks and dependency ordering. |

### Architecture

#### Logical Event Flow Diagram

The diagram below shows the **logical flow of events** through the system — how data moves from sources (Oracle, periodic health checks) through the event bus to the frontend. This is **not** an infrastructure/deployment diagram; see [docs/README.md](docs/README.md) for C4 architecture views and system context diagrams.

```
Oracle DB
  │  (INSERT into log_traza)
  ▼
Debezium / Kafka Connect  ──►  Kafka Topic (log_traza)
                                        │
                               KafkaConsumerService  ──►  EmailService (CRITICAL only, @Async)
                                        │
                                     EventBus  (ConcurrentHashMap + Virtual Threads)
                                        ▲
                               PollingService  ──────►  EmailService (CRITICAL only, @Async)
                               (health checks, @Scheduled every 30 s)
                                        │
                                  SseController
                                  GET /api/events/stream
                                        │
                                 Next.js Frontend
                                 useMonitor() hook
                                 EventSource API
```

**Event flow:**

1. Debezium detects a new row in `log_traza` and publishes a CDC message to Kafka.
2. `KafkaConsumerService` parses the Debezium envelope, builds a `UnifiedEvent`, and calls `EventBus.publish()`.
3. `PollingService` runs on a fixed schedule, checks each target, and calls `EventBus.publish()` on status changes.
4. `EventBus` fans out each `UnifiedEvent` to all active `SseEmitter` instances using **Virtual Threads** (one lightweight thread per client).
5. The browser's `EventSource` receives the event under its named channel (`"data"` or `"infrastructure"`) and the `useMonitor` hook updates the React state.

### Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 (Virtual Threads), Spring Boot 3.4.4, Maven |
| Messaging | Apache Kafka 4.2.0 + Debezium 3.5.0.Final (CDC) |
| Database | Oracle XE 11g |
| Email | Spring Mail (SMTP with TLS) |
| Security | Spring Security, Bucket4j (Rate Limiting) |
| Monitoring | Spring Boot Actuator (`/actuator/health`) |
| Frontend | Next.js 15, React 18, TypeScript, Tailwind CSS |
| Container | Docker multi-stage builds, Docker Compose |
| CI/CD | GitHub Actions |

### Prerequisites

| Tool | Minimum version |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9 |
| Node.js | 20 |
| npm | 9 |
| Docker & Docker Compose | 20 / 2 |

### Quick Start

#### 1. Clone the repository

```bash
git clone https://github.com/careb36/Monitor.git
cd Monitor
```

#### 2. Start the full stack with Docker Compose

Create a `.env` file at the project root (see `.env.example` if available):

```bash
ORACLE_PASSWORD=your_oracle_password
MONITOR_APP_USER=monitor_app
MONITOR_APP_PASSWORD=your_app_password
```

Then start everything:

```bash
docker compose up -d
```

This starts Oracle XE, Zookeeper, Kafka, Debezium, the backend, and the frontend — with health-based dependency ordering. The frontend waits for the backend to be healthy, and the backend waits for Kafka and Oracle.

#### 3. Access the dashboard

Open **http://localhost:3000** and click **"Start Monitoring"** to open the SSE connection.

#### Local development (without Docker)

**Backend:**

```bash
mvn --batch-mode clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend starts on **http://localhost:8080**.

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

The dashboard opens on **http://localhost:3000**. The `next.config.js` rewrites proxy `/api/*` requests to `localhost:8080` automatically.

### Docker

Both services use **multi-stage builds** to minimize image size:

- **Backend** — `maven:3.9.9-eclipse-temurin-21-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime, ~200 MB). Runs as non-root `monitor` user.
- **Frontend** — `node:20-alpine` (build) → `node:20-alpine` with Next.js **standalone output** (runtime, ~100 MB). Runs as non-root `nextjs` user.

**Docker Compose healthchecks:**

| Service | Health check | Start period |
|---|---|---|
| Oracle | `healthcheck.sh` | — |
| Kafka | `kafka-topics --list` | — |
| Debezium | `curl` to REST API | 60 s |
| Backend | `wget /actuator/health` | 40 s |
| Frontend | `wget http://localhost:3000` | 20 s |

**Dependency chain:** Oracle + Kafka (healthy) → Backend (healthy) → Frontend

**Networking:** All services communicate over a dedicated `monitor-net` bridge network. The frontend reaches the backend using Docker DNS (`http://backend:8080`) via a build-time `BACKEND_URL` argument baked into Next.js rewrites.

### API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/events/stream` | `MONITOR_USER` role | SSE stream with `data` and `infrastructure` named events. |
| `GET` | `/actuator/health` | Public | Application health status for Docker/load balancer probes. |

**`UnifiedEvent` payload:**

```json
{
  "type":      "DATA | INFRASTRUCTURE",
  "severity":  "INFO | WARNING | CRITICAL",
  "source":    "string",
  "message":   "string",
  "timestamp": "2025-06-01T12:00:00Z"
}
```

### Security

- **Authentication:** HTTP Basic (stateless, no session CSRF needed for GET-only SSE).
- **Authorization:** SSE stream requires `MONITOR_USER` role; admin endpoints require `MONITOR_ADMIN`.
- **Rate limiting:** Bucket4j throttles requests per time window (configurable).
- **Headers:** HSTS, CSP, X-Frame-Options (deny), X-Content-Type-Options, Referrer-Policy.
- **Health endpoint:** `/actuator/health` is publicly accessible (no sensitive details exposed).
- **Containers:** Both images run as non-root users.
- **Scanning:** OWASP Dependency-Check in Maven (fail on CVSS ≥ 8.0).

### Running Tests

```bash
# Backend: all unit tests
mvn --batch-mode test

# Backend: full build + test + security scan
mvn --batch-mode clean verify

# Frontend: lint
cd frontend && npm run lint
```

### Project Structure

```
Monitor/
├── src/main/java/com/monitor/
│   ├── App.java                        # Main class
│   ├── config/                         # CORS, Security, Jackson
│   ├── controller/SseController.java   # SSE endpoint
│   ├── model/                          # EventType, Severity, UnifiedEvent
│   ├── security/                       # Rate limiting
│   └── service/
│       ├── EmailService.java           # Async CRITICAL alerts
│       ├── EventBus.java               # SSE fan-out hub
│       ├── KafkaConsumerService.java   # CDC consumer
│       ├── PollingService.java         # Health-check poller
│       └── persistence/               # Outbox entities & repos
├── src/main/resources/application.yml  # All config (profiles: dev, staging, prod)
├── src/test/java/com/monitor/         # JUnit 5 + Mockito tests
├── frontend/
│   ├── src/app/                       # Next.js pages
│   ├── src/hooks/useMonitor.ts        # SSE hook
│   ├── src/lib/types.ts              # TypeScript types
│   ├── next.config.js                 # Standalone output + API rewrites
│   └── Dockerfile                     # Multi-stage (node:20-alpine)
├── docker/
│   ├── oracle/init.sql               # DB init + Debezium setup
│   └── debezium/connector-log-traza.json
├── docs/                              # Architecture, ADRs, runbooks, diagrams
├── Dockerfile                         # Backend multi-stage (temurin-21-alpine)
├── docker-compose.yml                 # Full stack with healthchecks
├── AGENTS.md                          # AI agent guardrails
├── CONTRIBUTING.md                    # Contribution workflow
└── CHANGELOG.md                       # Release history
```

### Branching Strategy (GitFlow)

| Branch | Purpose |
|---|---|
| `main` | Production-ready code (protected) |
| `develop` | Integration branch (protected) |
| `feature/*` | New features — from `develop` |
| `bugfix/*` | Non-critical fixes — from `develop` |
| `release/*` | Release preparation — merges into `main` + `develop` |
| `hotfix/*` | Emergency fixes — from `main` |

All changes to `main` and `develop` go through Pull Requests with CI approval.

### CI/CD

| Workflow | Trigger | Purpose |
|---|---|---|
| `ci.yml` | Push/PR to `develop`, `feature/*`, `bugfix/*` | Build & test |
| `release.yml` | Push/PR on `release/*`, `hotfix/*` | Build, test & tag |
| `deploy.yml` | Push to `main` | Build & deploy |
| `lint.yml` | Pull Requests | Conventional Commits & branch naming |

### Further Reading

- [CONTRIBUTING.md](CONTRIBUTING.md) — workflow, commit conventions, PR expectations
- [AGENTS.md](AGENTS.md) — guardrails for AI coding agents
- [docs/README.md](docs/README.md) — **detailed architecture diagrams** (C4 views, sequence diagrams, system context), ADRs (architectural decisions), and operational runbooks
- [CHANGELOG.md](CHANGELOG.md) — release history

**Architecture Diagram Types:**
- **Logical Flow** (this README, "Architecture" section) — shows how events move through services
- **C4 System Context** — high-level actors and system boundaries
- **C4 Container** — major components (Backend, Frontend, Kafka, Oracle)
- **C4 Component** — Java services and event bus internals
- **Sequence Diagrams** — SSE streaming and critical alert flows at runtime

### License

This project is licensed under the [Apache License 2.0](LICENSE).

---

<a id="español"></a>
## 🇪🇸 Español

### Descripción General

**Monitor** es un panel de monitoreo de operaciones en tiempo real que transmite eventos en vivo desde un backend Java/Spring Boot a un frontend Next.js/React utilizando **Server-Sent Events (SSE)**.

Proporciona una única vista centralizada de dos flujos de datos críticos:

- **Eventos de datos** — cambios capturados desde una base de datos Oracle mediante Debezium CDC (Change Data Capture) y Apache Kafka.
- **Eventos de infraestructura** — sondeos periódicos de salud a bases de datos secundarias y demonios en segundo plano, con alertas automáticas cuando un objetivo cae.

Los eventos críticos activan simultáneamente una alerta visual en el panel (con señal de audio) y una notificación por correo electrónico asíncrona a los destinatarios configurados.

### Características Principales

| Característica | Detalle |
|---|---|
| 🔴 **Streaming en tiempo real** | Los Server-Sent Events empujan actualizaciones instantáneamente a cada pestaña del navegador conectada, sin necesidad de polling desde el cliente. |
| 📋 **Integración CDC** | Debezium captura cada `INSERT` en la tabla Oracle `log_traza` y lo reenvía a Kafka, que el backend consume y retransmite. |
| 🩺 **Verificaciones de salud** | Sondeos programados (por defecto: cada 30 s) monitorean bases de datos y demonios, emitiendo eventos solo en transiciones de estado para reducir el ruido. |
| 🔔 **Alertas por correo** | Se envía un correo asíncrono por cada evento `CRITICAL` sin bloquear el pipeline de eventos. |
| 🔊 **Señales de audio** | El frontend reproduce una secuencia corta de pitidos (Web Audio API) cuando llega un evento `CRITICAL`. |
| 📊 **Panel de log en vivo** | La interfaz Next.js mantiene un log rotativo de hasta 100 eventos y un tablero de estado de infraestructura por fuente. |
| 🐳 **Docker Compose listo** | Stack completo (Oracle XE, Zookeeper, Kafka, Debezium, backend, frontend) con healthchecks y orden de dependencias. |

### Arquitectura

#### Diagrama de Flujo Lógico de Eventos

El diagrama de abajo muestra el **flujo lógico de eventos** a través del sistema — cómo se mueven los datos desde las fuentes (Oracle, verificaciones de salud periódicas) a través del event bus hasta el frontend. Esto **no es** un diagrama de infraestructura/despliegue; ver [docs/README.md](docs/README.md) para vistas arquitectónicas C4 y diagramas de contexto del sistema.

```
Oracle DB
  │  (INSERT en log_traza)
  ▼
Debezium / Kafka Connect  ──►  Kafka Topic (log_traza)
                                        │
                               KafkaConsumerService  ──►  EmailService (solo CRITICAL, @Async)
                                        │
                                     EventBus  (ConcurrentHashMap + Hilos Virtuales)
                                        ▲
                               PollingService  ──────►  EmailService (solo CRITICAL, @Async)
                               (verificaciones de salud, @Scheduled cada 30 s)
                                        │
                                  SseController
                                  GET /api/events/stream
                                        │
                                 Frontend Next.js
                                 hook useMonitor()
                                 EventSource API
```

**Flujo de eventos:**

1. Debezium detecta una nueva fila en `log_traza` y publica un mensaje CDC en Kafka.
2. `KafkaConsumerService` analiza el envelope de Debezium, construye un `UnifiedEvent` y llama a `EventBus.publish()`.
3. `PollingService` se ejecuta en intervalos fijos, verifica cada objetivo y llama a `EventBus.publish()` en cambios de estado.
4. `EventBus` distribuye cada `UnifiedEvent` a todos los `SseEmitter` activos mediante **Hilos Virtuales** (un hilo ligero por cliente).
5. El `EventSource` del navegador recibe el evento en su canal nombrado (`"data"` o `"infrastructure"`) y el hook `useMonitor` actualiza el estado de React.

### Pila Tecnológica

| Capa | Tecnología |
|---|---|
| Backend | Java 21 (Hilos Virtuales), Spring Boot 3.4.4, Maven |
| Mensajería | Apache Kafka 4.2.0 + Debezium 3.5.0.Final (CDC) |
| Base de datos | Oracle XE 11g |
| Correo | Spring Mail (SMTP con TLS) |
| Seguridad | Spring Security, Bucket4j (Rate Limiting) |
| Monitoreo | Spring Boot Actuator (`/actuator/health`) |
| Frontend | Next.js 15, React 18, TypeScript, Tailwind CSS |
| Contenedores | Docker multi-stage builds, Docker Compose |
| CI/CD | GitHub Actions |

### Requisitos Previos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9 |
| Node.js | 20 |
| npm | 9 |
| Docker y Docker Compose | 20 / 2 |

### Inicio Rápido

#### 1. Clonar el repositorio

```bash
git clone https://github.com/careb36/Monitor.git
cd Monitor
```

#### 2. Levantar el stack completo con Docker Compose

Crear un archivo `.env` en la raíz del proyecto (ver `.env.example` si está disponible):

```bash
ORACLE_PASSWORD=tu_password_oracle
MONITOR_APP_USER=monitor_app
MONITOR_APP_PASSWORD=tu_password_app
```

Luego levantar todo:

```bash
docker compose up -d
```

Esto inicia Oracle XE, Zookeeper, Kafka, Debezium, el backend y el frontend — con orden de dependencias basado en healthchecks. El frontend espera a que el backend esté healthy, y el backend espera a Kafka y Oracle.

#### 3. Acceder al panel

Abrir **http://localhost:3000** y hacer clic en **"Start Monitoring"** para abrir la conexión SSE.

#### Desarrollo local (sin Docker)

**Backend:**

```bash
mvn --batch-mode clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El backend se inicia en **http://localhost:8080**.

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

El panel se abre en **http://localhost:3000**. El `next.config.js` redirige las peticiones `/api/*` a `localhost:8080` automáticamente.

### Docker

Ambos servicios usan **multi-stage builds** para minimizar el tamaño de imagen:

- **Backend** — `maven:3.9.9-eclipse-temurin-21-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime, ~200 MB). Ejecuta como usuario no-root `monitor`.
- **Frontend** — `node:20-alpine` (build) → `node:20-alpine` con **standalone output** de Next.js (runtime, ~100 MB). Ejecuta como usuario no-root `nextjs`.

**Healthchecks en Docker Compose:**

| Servicio | Health check | Start period |
|---|---|---|
| Oracle | `healthcheck.sh` | — |
| Kafka | `kafka-topics --list` | — |
| Debezium | `curl` a REST API | 60 s |
| Backend | `wget /actuator/health` | 40 s |
| Frontend | `wget http://localhost:3000` | 20 s |

**Cadena de dependencias:** Oracle + Kafka (healthy) → Backend (healthy) → Frontend

**Red:** Todos los servicios se comunican sobre una red bridge dedicada `monitor-net`. El frontend llega al backend usando DNS de Docker (`http://backend:8080`) mediante un argumento de build `BACKEND_URL` incorporado en los rewrites de Next.js.

### Referencia de la API

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/events/stream` | Rol `MONITOR_USER` | Stream SSE con eventos nombrados `data` e `infrastructure`. |
| `GET` | `/actuator/health` | Público | Estado de salud para probes de Docker/balanceador. |

**Payload `UnifiedEvent`:**

```json
{
  "type":      "DATA | INFRASTRUCTURE",
  "severity":  "INFO | WARNING | CRITICAL",
  "source":    "string",
  "message":   "string",
  "timestamp": "2025-06-01T12:00:00Z"
}
```

### Seguridad

- **Autenticación:** HTTP Basic (stateless, sin CSRF para API de solo GET).
- **Autorización:** SSE requiere rol `MONITOR_USER`; endpoints admin requieren `MONITOR_ADMIN`.
- **Rate limiting:** Bucket4j limita peticiones por ventana de tiempo (configurable).
- **Headers:** HSTS, CSP, X-Frame-Options (deny), X-Content-Type-Options, Referrer-Policy.
- **Health endpoint:** `/actuator/health` es público (sin detalles sensibles expuestos).
- **Contenedores:** Ambas imágenes ejecutan como usuarios no-root.
- **Escaneo:** OWASP Dependency-Check en Maven (falla en CVSS ≥ 8.0).

### Ejecución de Tests

```bash
# Backend: todos los tests unitarios
mvn --batch-mode test

# Backend: build completo + tests + escaneo de seguridad
mvn --batch-mode clean verify

# Frontend: lint
cd frontend && npm run lint
```

### Estructura del Proyecto

```
Monitor/
├── src/main/java/com/monitor/
│   ├── App.java                        # Clase principal
│   ├── config/                         # CORS, Security, Jackson
│   ├── controller/SseController.java   # Endpoint SSE
│   ├── model/                          # EventType, Severity, UnifiedEvent
│   ├── security/                       # Rate limiting
│   └── service/
│       ├── EmailService.java           # Alertas CRITICAL async
│       ├── EventBus.java               # Hub de fan-out SSE
│       ├── KafkaConsumerService.java   # Consumidor CDC
│       ├── PollingService.java         # Poller de health-checks
│       └── persistence/               # Entidades y repos del Outbox
├── src/main/resources/application.yml  # Config (perfiles: dev, staging, prod)
├── src/test/java/com/monitor/         # Tests JUnit 5 + Mockito
├── frontend/
│   ├── src/app/                       # Páginas Next.js
│   ├── src/hooks/useMonitor.ts        # Hook SSE
│   ├── src/lib/types.ts              # Tipos TypeScript
│   ├── next.config.js                 # Standalone output + API rewrites
│   └── Dockerfile                     # Multi-stage (node:20-alpine)
├── docker/
│   ├── oracle/init.sql               # Init DB + setup Debezium
│   └── debezium/connector-log-traza.json
├── docs/                              # Arquitectura, ADRs, runbooks, diagramas
├── Dockerfile                         # Backend multi-stage (temurin-21-alpine)
├── docker-compose.yml                 # Stack completo con healthchecks
├── AGENTS.md                          # Guardrails para agentes IA
├── CONTRIBUTING.md                    # Flujo de contribución
└── CHANGELOG.md                       # Historial de cambios
```

### Estrategia de Ramas (GitFlow)

| Rama | Propósito |
|---|---|
| `main` | Código listo para producción (protegida) |
| `develop` | Rama de integración (protegida) |
| `feature/*` | Nuevas funcionalidades — desde `develop` |
| `bugfix/*` | Correcciones no críticas — desde `develop` |
| `release/*` | Preparación de versiones — se fusiona en `main` + `develop` |
| `hotfix/*` | Correcciones urgentes — desde `main` |

Todos los cambios a `main` y `develop` pasan por Pull Requests con aprobación de CI.

### CI/CD

| Flujo de Trabajo | Disparador | Propósito |
|---|---|---|
| `ci.yml` | Push/PR a `develop`, `feature/*`, `bugfix/*` | Compilar y probar |
| `release.yml` | Push/PR en `release/*`, `hotfix/*` | Compilar, probar y etiquetar |
| `deploy.yml` | Push a `main` | Compilar y desplegar |
| `lint.yml` | Pull Requests | Conventional Commits y nomenclatura de ramas |

### Lectura Adicional

- [CONTRIBUTING.md](CONTRIBUTING.md) — flujo de trabajo, convenciones de commits, expectativas de PR
- [AGENTS.md](AGENTS.md) — guardrails para agentes de IA
- [docs/README.md](docs/README.md) — **diagramas detallados de arquitectura** (vistas C4, diagramas de secuencia, contexto del sistema), ADRs (decisiones arquitectónicas) y runbooks operacionales
- [CHANGELOG.md](CHANGELOG.md) — historial de cambios

**Tipos de diagramas de arquitectura:**
- **Flujo Lógico** (este README, sección "Arquitectura") — muestra cómo se mueven los eventos a través de servicios
- **C4 System Context** — actores de alto nivel y límites del sistema
- **C4 Container** — componentes principales (Backend, Frontend, Kafka, Oracle)
- **C4 Component** — servicios Java e internals del event bus
- **Sequence Diagrams** — flujos de SSE y alertas críticas en tiempo de ejecución

### Licencia

Este proyecto está bajo la [Licencia Apache 2.0](LICENSE).
