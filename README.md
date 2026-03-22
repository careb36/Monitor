# Monitor

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

A monitoring application built with Java.

### Branching Strategy

This project follows the **GitFlow** branching model.

| Branch        | Purpose                                      |
|---------------|----------------------------------------------|
| `main`        | Production-ready code (protected)            |
| `develop`     | Integration branch for features (protected)  |
| `feature/*`   | New features — branches from `develop`       |
| `bugfix/*`    | Non-critical fixes — branches from `develop` |
| `release/*`   | Release preparation — merges into `main` + `develop` |
| `hotfix/*`    | Emergency production fixes — branches from `main`    |

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full workflow documentation, commit conventions, and PR guidelines.

### Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

### CI/CD

| Workflow      | Trigger                              | Purpose                  |
|---------------|--------------------------------------|--------------------------|
| CI – Feature  | Push/PR to `develop`, `feature/*`, `bugfix/*` | Build & test |
| CI – Release  | Push/PR on `release/*`, `hotfix/*`  | Build, test & tag        |
| CD – Deploy   | Push to `main`                       | Build & deploy           |

---

<a id="español"></a>
## 🇪🇸 Español

Una aplicación de monitoreo construida con Java.

### Estrategia de Ramas

Este proyecto sigue el modelo de ramas **GitFlow**.

| Rama          | Propósito                                              |
|---------------|--------------------------------------------------------|
| `main`        | Código listo para producción (protegida)               |
| `develop`     | Rama de integración para funcionalidades (protegida)   |
| `feature/*`   | Nuevas funcionalidades — se crean desde `develop`      |
| `bugfix/*`    | Correcciones no críticas — se crean desde `develop`    |
| `release/*`   | Preparación de versiones — se fusiona en `main` + `develop` |
| `hotfix/*`    | Correcciones urgentes de producción — se crean desde `main` |

Consulta [CONTRIBUTING.md](CONTRIBUTING.md) para la documentación completa del flujo de trabajo, convenciones de commits y guías para Pull Requests.

### Historial de Cambios

Consulta [CHANGELOG.md](CHANGELOG.md) para ver el historial de versiones.

### CI/CD

| Flujo de Trabajo | Disparador                                        | Propósito                       |
|------------------|---------------------------------------------------|---------------------------------|
| CI – Feature     | Push/PR a `develop`, `feature/*`, `bugfix/*`      | Compilar y probar               |
| CI – Release     | Push/PR en `release/*`, `hotfix/*`                | Compilar, probar y etiquetar    |
| CD – Deploy      | Push a `main`                                     | Compilar y desplegar            |
