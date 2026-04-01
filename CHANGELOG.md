# Changelog

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [Unreleased]

#### Added
- EventBus stress test suite for validating 10,000+ concurrent clients.
- Global Virtual Threads enablement in `application.yml`.

#### Changed
- Refactored `EventBus` to use `ConcurrentHashMap.newKeySet()` and Virtual Threads for parallel broadcast. This resolves severe GC pressure and sequential blocking issues for large-scale SSE fan-out.
- Updated `EventBusTest` to support asynchronous event verification using `Awaitility`.

---

<a id="español"></a>
## 🇪🇸 Español

Todos los cambios importantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), y el proyecto sigue [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

### [Sin versión]

#### Agregado
- Suite de pruebas de estrés para `EventBus` validando 10.000+ clientes concurrentes.
- Habilitación global de hilos virtuales (Virtual Threads) en `application.yml`.

#### Modificado
- Refactorización de `EventBus` utilizando `ConcurrentHashMap.newKeySet()` e hilos virtuales para el despacho paralelo. Esto resuelve problemas críticos de presión de GC y bloqueo secuencial en el fan-out de SSE a gran escala.
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
