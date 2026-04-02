# Changelog

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [Unreleased]

#### Security
- Closed security hardening findings #8 to #14 for the current phase and promoted the release from `develop` to `main`.
- Stabilized Kafka SASL_SSL rollout by fixing cp-kafka ZooKeeper readiness behavior in secure startup.
- Added broker SCRAM bootstrap requirements and validation in secure preflight.
- Regenerated broker certificate flow with SANs (`kafka`, `localhost`, `monitor-kafka`) to prevent TLS hostname mismatch during secure bootstrap.

#### Documentation
- Updated operations and readiness documentation to reflect final phase closure, secure smoke evidence, and release promotion completion.

#### Added
- Spring Boot Actuator health endpoint (`/actuator/health`) for Docker healthcheck probes.
- Docker Compose healthchecks for backend (`wget /actuator/health`) and frontend (`wget :3000`).
- Frontend `depends_on: backend: service_healthy` — frontend waits for backend to be ready.
- Backend `depends_on` with `service_healthy` for Kafka and Oracle.
- `.dockerignore` files for backend and frontend to reduce build context size.
- `BACKEND_URL` build argument in frontend Dockerfile for Docker DNS resolution.
- EventBus stress test suite for validating 10,000+ concurrent clients.
- Global Virtual Threads enablement in `application.yml`.

#### Changed
- Frontend Dockerfile now uses Next.js **standalone output** — image reduced from ~500 MB to ~100 MB.
- Frontend `next.config.js` rewrites read `BACKEND_URL` env var (default: `http://localhost:8080`).
- Removed unused `NEXT_PUBLIC_API_URL` environment variable from Docker Compose.
- `/actuator/health` permitted without authentication in production security profile.
- Refactored `EventBus` to use `ConcurrentHashMap.newKeySet()` and Virtual Threads for parallel broadcast.
- Updated `EventBusTest` to support asynchronous event verification using `Awaitility`.

---

<a id="español"></a>
## 🇪🇸 Español

Todos los cambios importantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), y el proyecto sigue [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

### [Sin versión]

#### Agregado
- Endpoint de salud Spring Boot Actuator (`/actuator/health`) para healthcheck probes de Docker.
- Healthchecks en Docker Compose para backend (`wget /actuator/health`) y frontend (`wget :3000`).
- Frontend `depends_on: backend: service_healthy` — el frontend espera a que el backend esté listo.
- Backend `depends_on` con `service_healthy` para Kafka y Oracle.
- Archivos `.dockerignore` para backend y frontend para reducir el tamaño del build context.
- Argumento de build `BACKEND_URL` en el Dockerfile del frontend para resolución DNS de Docker.
- Suite de pruebas de estrés para `EventBus` validando 10.000+ clientes concurrentes.
- Habilitación global de hilos virtuales (Virtual Threads) en `application.yml`.

#### Modificado
- El Dockerfile del frontend ahora usa **standalone output** de Next.js — imagen reducida de ~500 MB a ~100 MB.
- Los rewrites de `next.config.js` leen la env var `BACKEND_URL` (default: `http://localhost:8080`).
- Eliminada la variable de entorno `NEXT_PUBLIC_API_URL` no utilizada del Docker Compose.
- `/actuator/health` permitido sin autenticación en el perfil de seguridad de producción.
- Refactorización de `EventBus` utilizando `ConcurrentHashMap.newKeySet()` e hilos virtuales para el despacho paralelo.
- Actualización de `EventBusTest` para soportar la verificación asincrónica de eventos mediante `Awaitility`.

#### Agregado (Previo)
- Estrategia de ramas GitFlow con `main`, `develop`, `feature/`, `release/`, `hotfix/` y `bugfix/`

---

<!--
Template for future releases / Plantilla para futuras versiones:

## [x.y.z] - YYYY-MM-DD

### Added / Agregado
-

### Changed / Modificado
-

### Deprecated / Obsoleto
-

### Removed / Eliminado
-

### Fixed / Corregido
-

### Security / Seguridad
-
-->

[Unreleased]: https://github.com/careb36/Monitor/compare/main...develop
