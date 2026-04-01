# 🏛️ Monitor: Global Instructions & Agency Culture

## 🌟 Project Overview & Identity
Monitor is a real-time operations monitoring dashboard for high-stakes environments. 
**Agency Philosophy:** We are strict with memory, obsessed with Java 21 performance (Virtual Threads), and we prioritize banking-grade security (BCU Style).

---

## 🛡️ Company Culture (The "How We Work")

### 1. Memory & Stability (Strict Mode)
- **Bounded Buffers:** The `CriticalOutbox` (in-memory) MUST NEVER exceed 500 entries. 
- **Backpressure:** If the buffer is full, use an explicit `BLOCKING_WAIT` or `FAIL_FAST` strategy. No unbounded growth.
- **Leak Prevention:** Always check for `SseEmitter` timeouts and proper removal from `EventBus`.

### 2. Banking Security (BCU Style)
- **Zero Injection:** No dynamic SQL strings. Use JPA or Parameterized Queries only.
- **PII Protection:** Never log sensitive data. Redact names/IDs before logging to SLF4J.
- **Audit:** Every `CRITICAL` event must be persisted in the Outbox before delivery.

### 3. Java 21 Modernization
- **Virtual Threads:** Use them for I/O bound tasks (Email, SSE delivery). 
- **Avoid Pinning:** Do not use `synchronized` blocks for I/O; use `ReentrantLock`.
- **Records:** Use `records` for DTOs and internal events (like `UnifiedEvent`).

---

## 🔄 Agent Communication Pipeline (Workflow)

To maintain architectural integrity, follow this delegation flow:
1. **The Architect (Gemini):** Analyzes specs and generates a `WORK_ORDER.md` with atomic tasks.
2. **The Developer (Copilot CLI):** Reads the `WORK_ORDER.md` and executes one task at a time.
3. **The Reviewer (Caro):** Supervises the logic. No task is "Done" until it passes the Memory and Security checks defined above.

---

## 🏗️ Architecture & Tech Stack
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

| Layer     | Technology                                      |
|-----------|-------------------------------------------------|
| Backend   | Java 21 (Virtual Threads), Spring Boot 4.0.5    |
| Messaging | Apache Kafka (`spring-kafka`)                   |
| Email     | Spring Mail (`spring-boot-starter-mail`)        |
| Persistence| Spring Data JPA (H2 for tests)                 |
| Security  | Spring Security, Bucket4j (Rate Limiting)      |
| Scanning  | OWASP Dependency Check, SpotBugs (FindSecBugs)  |
| Frontend  | Next.js 15, React 18, TypeScript                |
| Container | Docker / Docker Compose                         |

---

## Repository Structure

```
Monitor/
├── src/
│   ├── main/java/com/monitor/
│   │   ├── App.java                      # Main class
│   │   ├── config/                       # Cors, Security, Jackson
│   │   ├── controller/SseController.java
│   │   ├── model/
│   │   │   ├── EventType.java            # Enum: DATA, INFRASTRUCTURE, …
│   │   │   ├── Severity.java             # Enum: INFO, WARNING, CRITICAL
│   │   │   └── UnifiedEvent.java
│   │   └── service/
│   │       ├── EmailService.java
│   │       ├── EventBus.java
│   │       ├── KafkaConsumerService.java
│   │       ├── PollingService.java
│   │       └── persistence/               # Outbox Entities & Repositories
│   └── test/java/com/monitor/
│       ├── AppTest.java
│       └── service/                      # Unit & Integration tests
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

### Backend (Maven 3.9+)

```bash
# Build and run all tests (includes security scans)
mvn --batch-mode clean verify

# Run tests only
mvn --batch-mode test

# Package the JAR
mvn --batch-mode clean package
```

CI uses **JDK 21** and requires **Maven 3.9+**.

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

- **Java**: Follow standard Spring Boot conventions. Use **Java 21** features where appropriate (records, sealed classes, virtual threads).
- **Security**: All new services must consider rate limiting and audit logging. Do not bypass security filters.
- **Tests**: Unit tests live in `src/test/java/com/monitor/service/`. Integration tests (JPA/Kafka) are encouraged for critical paths.
- **Frontend**: TypeScript throughout. Custom React hooks live in `frontend/src/hooks/`. Use named exports.
- **SSE events**: When adding a new event type, add a value to `EventType` enum. The lowercase enum name becomes the SSE event name. Update `useMonitor.ts` to listen for the new event.
- **New services**: Register new publishers with `EventBus` by injecting it and calling `publish(UnifiedEvent)`.
- **No secrets in source**: Use environment variables or Spring `application.properties` for credentials and configuration.
