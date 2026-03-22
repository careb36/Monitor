---
name: Release Request / Solicitud de Release
about: Request a new release version / Solicita una nueva versión de release
title: "release: v"
labels: release
assignees: ""
---

## Release Version / Versión de Release

<!-- e.g., v1.2.0 / p. ej., v1.2.0 -->

## Included Features / Fixes / Funcionalidades y Correcciones Incluidas

<!-- List the features and fixes included in this release -->
<!-- Lista las funcionalidades y correcciones incluidas en esta versión -->

- 
- 

## Pre-release Checklist / Lista de Verificación Pre-release

- [ ] All feature branches merged into `develop` / Todas las ramas de funcionalidades fusionadas en `develop`
- [ ] `release/<version>` branch created from `develop` / Rama `release/<version>` creada desde `develop`
- [ ] All tests pass on the release branch / Todos los tests pasan en la rama de release
- [ ] Documentation updated / Documentación actualizada
- [ ] CHANGELOG updated / CHANGELOG actualizado
- [ ] PR opened: `release/<version>` → `main` / PR abierto: `release/<version>` → `main`
- [ ] PR opened: `release/<version>` → `develop` / PR abierto: `release/<version>` → `develop`
- [ ] Version tag created on `main` after merge / Etiqueta de versión creada en `main` después del merge
