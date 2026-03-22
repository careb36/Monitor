# Copilot Instructions for Monitor

## Project Overview

Monitor is a real-time operations monitoring dashboard. The backend is built with **Spring Boot 3.2.5 (Java 17)** and uses **Server-Sent Events (SSE)** to push live data to a **Next.js 15 / React 18** frontend written in TypeScript.

---

## Architecture

```
Kafka ──► KafkaConsumerService ──►
                                   EventBus (CopyOnWriteArrayList<SseEmitter>)
PollingService ────────────────►        │
                                        ▼
                               SseController  GET /api/events/stream
                                        │
                               EmailService  (async, CRITICAL events only)
```

- **EventBus** (`com.monitor.service.EventBus`) is the central hub that holds SSE emitters.
- **KafkaConsumerService** and **PollingService** both publish `UnifiedEvent` objects to the `EventBus`.
- **SseController** exposes `GET /api/events/stream` and registers new `SseEmitter` instances with the `EventBus`.
- **EmailService** is `@Async` and only fires for events with `Severity.CRITICAL`.
- SSE event names match the lowercase `EventType` enum values (e.g., `"data"`, `"infrastructure"`).
- The Next.js frontend subscribes via `useMonitor` hook and listens to those named SSE events.

---

## Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Backend   | Java 17, Spring Boot 3.2.5, Maven       |
| Messaging | Apache Kafka (`spring-kafka`)           |
| Email     | Spring Mail (`spring-boot-starter-mail`)|
| Frontend  | Next.js 15, React 18, TypeScript        |
| Container | Docker / Docker Compose                 |

---

## Repository Structure

```
Monitor/
├── src/
│   ├── main/java/com/monitor/
│   │   ├── App.java                      # Main class
│   │   ├── config/CorsConfig.java
│   │   ├── controller/SseController.java
│   │   ├── model/
│   │   │   ├── EventType.java            # Enum: DATA, INFRASTRUCTURE, …
│   │   │   ├── Severity.java             # Enum: INFO, WARNING, CRITICAL
│   │   │   └── UnifiedEvent.java
│   │   └── service/
│   │       ├── EmailService.java
│   │       ├── EventBus.java
│   │       ├── KafkaConsumerService.java
│   │       └── PollingService.java
│   └── test/java/com/monitor/
│       ├── AppTest.java
│       └── service/                      # 10 unit tests, no Spring context
├── frontend/                             # Next.js app
│   └── src/
│       ├── app/
│       ├── hooks/useMonitor.ts
│       └── lib/
├── docker/
├── docker-compose.yml
├── pom.xml
└── .github/
    ├── workflows/
    │   ├── ci.yml        # Build & test on feature/bugfix/develop branches
    │   ├── release.yml   # Build, test & tag on release/hotfix branches
    │   ├── deploy.yml    # Build & deploy on push to main
    │   ├── lint.yml      # PR title (Conventional Commits) & branch name (GitFlow)
    │   └── init.yml      # workflow_dispatch: creates develop from main
    ├── ISSUE_TEMPLATE/
    └── PULL_REQUEST_TEMPLATE.md
```

---

## Build & Test Commands

### Backend (Maven)

```bash
# Build and run all tests
mvn --batch-mode clean verify

# Run tests only
mvn --batch-mode test

# Package the JAR
mvn --batch-mode clean package
```

CI uses **JDK 17** with `actions/setup-java@v4` and `cache: maven`.

### Frontend (npm / Next.js)

```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Production build
npm run build

# Lint
npm run lint
```

---

## Branching Strategy (GitFlow)

| Branch        | Purpose                                      |
|---------------|----------------------------------------------|
| `main`        | Production-ready code (protected, no direct push) |
| `develop`     | Integration branch for features (protected)  |
| `feature/*`   | New features — branches from `develop`       |
| `bugfix/*`    | Non-critical fixes — branches from `develop` |
| `release/*`   | Release prep — merges into `main` + `develop`|
| `hotfix/*`    | Emergency fixes — branches from `main`       |

All changes to `main` and `develop` must go through a Pull Request with at least 1 approval and passing CI.

---

## Commit Message Convention (Conventional Commits)

```
<type>(<scope>): <short description>
```

| Type       | When to use                                      |
|------------|--------------------------------------------------|
| `feat`     | New feature                                      |
| `fix`      | Bug fix                                          |
| `docs`     | Documentation only                               |
| `style`    | Formatting, no logic change                      |
| `refactor` | Code restructuring                               |
| `test`     | Adding or updating tests                         |
| `chore`    | Build, deps, CI changes                          |
| `perf`     | Performance improvements                         |
| `ci`       | CI/CD configuration changes                      |

PR titles must also follow this convention (enforced by `lint.yml`).

---

## Coding Guidelines

- **Java**: Follow standard Spring Boot conventions. Services are `@Service` beans. Async methods use `@Async`. Scheduled methods use `@Scheduled`.
- **Tests**: Unit tests live in `src/test/java/com/monitor/service/`. Tests do **not** load the Spring context — use plain JUnit 5 with Mockito.
- **Frontend**: TypeScript throughout. Custom React hooks live in `frontend/src/hooks/`. Use named exports.
- **SSE events**: When adding a new event type, add a value to `EventType` enum. The lowercase enum name becomes the SSE event name. Update `useMonitor.ts` to listen for the new event.
- **New services**: Register new publishers with `EventBus` by injecting it and calling `publish(UnifiedEvent)`.
- **No secrets in source**: Use environment variables or Spring `application.properties` for credentials and configuration.
