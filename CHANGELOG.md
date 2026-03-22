# Changelog

🇬🇧 All notable changes to this project will be documented in this file.  
🇪🇸 Todos los cambios importantes de este proyecto se documentarán en este archivo.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
y este proyecto sigue el [Versionado Semántico](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] / [Sin versión]

### Added / Agregado
- GitFlow branching strategy with `main`, `develop`, `feature/`, `release/`, `hotfix/`, and `bugfix/` branches  
  Estrategia de ramas GitFlow con `main`, `develop`, `feature/`, `release/`, `hotfix/` y `bugfix/`
- GitHub Actions CI workflows for feature, develop, release, and hotfix branches  
  Flujos de CI con GitHub Actions para ramas feature, develop, release y hotfix
- GitHub Actions CD workflow for automated deployment from `main`  
  Flujo de CD con GitHub Actions para despliegue automático desde `main`
- Pull Request template following GitFlow conventions  
  Plantilla de Pull Request siguiendo las convenciones de GitFlow
- Issue templates: Bug Report, Feature Request, Release Request  
  Plantillas de issues: Reporte de Bug, Solicitud de Funcionalidad, Solicitud de Release
- `CONTRIBUTING.md` with full GitFlow documentation and commit conventions  
  `CONTRIBUTING.md` con documentación completa de GitFlow y convenciones de commits
- Automatic version tagging for `hotfix/**` branches (reads version from `pom.xml`)  
  Etiquetado automático de versiones para ramas `hotfix/**` (lee la versión desde `pom.xml`)
- Lint workflow to validate PR titles against Conventional Commits and enforce GitFlow branch naming  
  Flujo de lint para validar títulos de PR contra Conventional Commits y nomenclatura de ramas GitFlow
- Init workflow to create the `develop` branch from `main` via `workflow_dispatch`  
  Flujo de inicialización para crear la rama `develop` desde `main` mediante `workflow_dispatch`
- Bilingual documentation (English / Spanish) across all project docs  
  Documentación bilingüe (inglés / español) en todos los documentos del proyecto

---

<!-- 
Template for future releases / Plantilla para futuras versiones:

## [x.y.z] – YYYY-MM-DD

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
