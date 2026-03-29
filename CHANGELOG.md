# Changelog

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [Unreleased]

#### Added
- GitFlow branching strategy with `main`, `develop`, `feature/`, `release/`, `hotfix/`, and `bugfix/` branches
- GitHub Actions CI workflows for feature, develop, release, and hotfix branches
- GitHub Actions CD workflow for automated deployment from `main`
- Pull Request template following GitFlow conventions
- Issue templates for bugs, features, and releases
- `CONTRIBUTING.md` with GitFlow workflow and commit conventions
- Automatic version tagging for `hotfix/**` branches using the version from `pom.xml`
- Lint workflow to validate PR titles and enforce branch naming conventions
- Init workflow to create the `develop` branch from `main` via `workflow_dispatch`
- Bilingual project documentation baseline in English and Spanish

---

<a id="español"></a>
## 🇪🇸 Español

Todos los cambios importantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), y el proyecto sigue [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

### [Sin versión]

#### Agregado
- Estrategia de ramas GitFlow con `main`, `develop`, `feature/`, `release/`, `hotfix/` y `bugfix/`
- Flujos de CI con GitHub Actions para ramas feature, develop, release y hotfix
- Flujo de CD con GitHub Actions para despliegue automático desde `main`
- Plantilla de Pull Request siguiendo las convenciones de GitFlow
- Plantillas de issues para bugs, funcionalidades y releases
- `CONTRIBUTING.md` con flujo GitFlow y convenciones de commits
- Etiquetado automático de versiones para ramas `hotfix/**` usando la versión de `pom.xml`
- Flujo de lint para validar títulos de PR y reforzar la nomenclatura de ramas
- Flujo de inicialización para crear la rama `develop` desde `main` mediante `workflow_dispatch`
- Línea base de documentación bilingüe en inglés y español

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
