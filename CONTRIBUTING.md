# Contributing to Monitor

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

Thank you for your interest in contributing! This document describes our branching strategy and contribution workflow.

---

### Branching Strategy: GitFlow

We follow the **GitFlow** branching model, a well-established workflow that provides a robust framework for managing releases, features, and hotfixes.

#### Main Branches

| Branch    | Purpose                                    | Direct pushes |
|-----------|--------------------------------------------|---------------|
| `main`    | Production-ready code. Every commit here is a release. | ❌ Never |
| `develop` | Integration branch. All features merge here first.     | ❌ Never |

Both `main` and `develop` are **protected branches**. All changes must go through a Pull Request.

---

#### `feature/`
- **Purpose:** New features or improvements.
- **Branches from:** `develop`
- **Merges into:** `develop`
- **Naming:** `feature/<short-description>` — e.g., `feature/user-authentication`

```bash
# Start a feature
git checkout develop
git pull origin develop
git checkout -b feature/my-new-feature

# Finish a feature (via Pull Request to develop)
git push origin feature/my-new-feature
# → Open a PR: feature/my-new-feature → develop
```

---

#### `release/`
- **Purpose:** Prepare a new production release. Only bug fixes, documentation, and release-oriented tasks go here. No new features.
- **Branches from:** `develop`
- **Merges into:** `main` AND `develop`
- **Naming:** `release/<version>` — e.g., `release/1.2.0`

```bash
# Start a release
git checkout develop
git pull origin develop
git checkout -b release/1.2.0

# After QA and final fixes, open two PRs:
# 1. release/1.2.0 → main  (tag with version after merge)
# 2. release/1.2.0 → develop
```

---

#### `hotfix/`
- **Purpose:** Critical bug fixes that need to go directly to production without waiting for the next release cycle.
- **Branches from:** `main`
- **Merges into:** `main` AND `develop`
- **Naming:** `hotfix/<short-description>` — e.g., `hotfix/fix-null-pointer`

```bash
# Start a hotfix
git checkout main
git pull origin main
git checkout -b hotfix/critical-bug

# After fix, open two PRs:
# 1. hotfix/critical-bug → main  (tag with patch version after merge)
# 2. hotfix/critical-bug → develop
```

---

#### `bugfix/`
- **Purpose:** Non-critical bug fixes that can wait for the next release.
- **Branches from:** `develop`
- **Merges into:** `develop`
- **Naming:** `bugfix/<short-description>` — e.g., `bugfix/incorrect-calculation`

```bash
git checkout develop
git pull origin develop
git checkout -b bugfix/incorrect-calculation

# Open a PR: bugfix/incorrect-calculation → develop
```

---

### Branch Flow Diagram

```
main ─────────────────────────────────────────────────► (production)
  │                    ▲                  ▲
  │              release/x.x.x       hotfix/xxx
  │                    │                  │
develop ──────────────┼──────────────────┼──────────────►
  ▲        ▲          │                  │
  │        │          │                  │
feature/  bugfix/   (branches from develop)
  xxx       xxx
```

---

### GitFlow Tooling Recommendation

**No plugin or external tool is required.** This repository relies on plain `git` commands and GitHub Actions for all GitFlow automation:

| Mechanism | Purpose |
|-----------|---------|
| `init.yml` workflow | Creates `develop` from `main` and removes stale branches (run once via **Actions → Init**) |
| `lint.yml` workflow | Validates PR titles (Conventional Commits) and branch names on every PR |
| `ci.yml` / `release.yml` | Run tests and publish releases |

If you prefer local tooling to speed up branch management, [**git-flow AVH Edition**](https://github.com/petervanderdoes/gitflow-avh) is the recommended option — it is actively maintained and wraps the same native `git` commands documented above:

```bash
# Install (macOS)
brew install git-flow-avh

# Install (Ubuntu/Debian)
apt-get install git-flow

# Initialise in the repo (accepts all defaults)
git flow init -d

# Examples
git flow feature start my-feature   # → feature/my-feature from develop
git flow feature finish my-feature  # → merges back into develop
git flow hotfix start 1.0.1         # → hotfix/1.0.1 from main
```

> **Recommendation:** Use plain `git` + the GitHub Actions workflows for day-to-day work. The AVH plugin is a convenience wrapper — it does not add any capability that the documented native commands don't already provide.

---

### Version Naming

We follow **Semantic Versioning** ([semver.org](https://semver.org)):

```
MAJOR.MINOR.PATCH
  │      │     └── Bug fixes / hotfixes
  │      └──────── New backward-compatible features
  └─────────────── Breaking changes
```

---

### Commit Message Convention

We follow the **Conventional Commits** specification ([conventionalcommits.org](https://www.conventionalcommits.org)):

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

#### Types

| Type       | Description                                      |
|------------|--------------------------------------------------|
| `feat`     | A new feature                                    |
| `fix`      | A bug fix                                        |
| `docs`     | Documentation changes only                       |
| `style`    | Code formatting (no logic changes)               |
| `refactor` | Code restructuring (no feature or fix)           |
| `test`     | Adding or updating tests                         |
| `chore`    | Build process, dependency updates, CI changes    |
| `perf`     | Performance improvements                         |
| `ci`       | CI/CD configuration changes                      |

#### Examples

```
feat(auth): add JWT-based authentication
fix(monitor): resolve null pointer on startup
docs(contributing): add branching strategy section
chore(deps): update dependencies to latest versions
```

---

### Pull Request Guidelines

1. **One PR per feature/fix** — keep PRs focused and small.
2. **Fill in the PR template** — describe the change, how to test it, and reference related issues.
3. **Require at least 1 approval** before merging.
4. **All CI checks must pass** before merging.
5. **Squash commits** when merging feature branches to `develop` to keep history clean.
6. **No force pushes** on `main` or `develop`.

---

### Getting Started

Before coding with AI assistants, read [AGENTS.md](AGENTS.md) first for repository-specific guardrails.

```bash
# 1. Fork the repository (if external contributor)
# 2. Clone your fork
git clone https://github.com/<your-username>/Monitor.git
cd Monitor

# 3. Add upstream remote
git remote add upstream https://github.com/careb36/Monitor.git

# 4. Always start from an up-to-date develop
git fetch upstream
git checkout develop
git merge upstream/develop

# 5. Create your branch following naming conventions above
git checkout -b feature/my-awesome-feature

# 6. Make your changes, commit, and push
git add .
git commit -m "feat(scope): short description"
git push origin feature/my-awesome-feature

# 7. Open a Pull Request to develop
```

---

### Code of Conduct

Please be respectful and constructive in all interactions. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) if present, or follow the [Contributor Covenant](https://www.contributor-covenant.org).

---

<a id="español"></a>
## 🇪🇸 Español

¡Gracias por tu interés en contribuir! Este documento describe nuestra estrategia de ramas y el flujo de trabajo para contribuciones.

---

### Estrategia de Ramas: GitFlow

Seguimos el modelo de ramas **GitFlow**, un flujo de trabajo consolidado que ofrece un marco robusto para gestionar versiones, funcionalidades y hotfixes.

#### Ramas Principales

| Rama      | Propósito                                                     | Pushes directos |
|-----------|---------------------------------------------------------------|-----------------|
| `main`    | Código listo para producción. Cada commit aquí es una versión. | ❌ Nunca |
| `develop` | Rama de integración. Todas las funcionalidades se fusionan aquí primero. | ❌ Nunca |

Tanto `main` como `develop` son **ramas protegidas**. Todos los cambios deben hacerse a través de un Pull Request.

---

#### `feature/`
- **Propósito:** Nuevas funcionalidades o mejoras.
- **Se crea desde:** `develop`
- **Se fusiona en:** `develop`
- **Nomenclatura:** `feature/<descripcion-corta>` — p. ej., `feature/autenticacion-usuario`

```bash
# Iniciar una funcionalidad
git checkout develop
git pull origin develop
git checkout -b feature/mi-nueva-funcionalidad

# Finalizar una funcionalidad (mediante Pull Request a develop)
git push origin feature/mi-nueva-funcionalidad
# → Abrir un PR: feature/mi-nueva-funcionalidad → develop
```

---

#### `release/`
- **Propósito:** Preparar una nueva versión de producción. Aquí solo se permiten correcciones de bugs, documentación y tareas orientadas a la versión. No se aceptan nuevas funcionalidades.
- **Se crea desde:** `develop`
- **Se fusiona en:** `main` Y `develop`
- **Nomenclatura:** `release/<version>` — p. ej., `release/1.2.0`

```bash
# Iniciar una release
git checkout develop
git pull origin develop
git checkout -b release/1.2.0

# Tras el QA y correcciones finales, abrir dos PRs:
# 1. release/1.2.0 → main  (etiquetar con la versión después del merge)
# 2. release/1.2.0 → develop
```

---

#### `hotfix/`
- **Propósito:** Correcciones críticas de bugs que necesitan ir directamente a producción sin esperar al siguiente ciclo de release.
- **Se crea desde:** `main`
- **Se fusiona en:** `main` Y `develop`
- **Nomenclatura:** `hotfix/<descripcion-corta>` — p. ej., `hotfix/corregir-puntero-nulo`

```bash
# Iniciar un hotfix
git checkout main
git pull origin main
git checkout -b hotfix/bug-critico

# Tras la corrección, abrir dos PRs:
# 1. hotfix/bug-critico → main  (etiquetar con versión de parche después del merge)
# 2. hotfix/bug-critico → develop
```

---

#### `bugfix/`
- **Propósito:** Correcciones de bugs no críticas que pueden esperar a la próxima versión.
- **Se crea desde:** `develop`
- **Se fusiona en:** `develop`
- **Nomenclatura:** `bugfix/<descripcion-corta>` — p. ej., `bugfix/calculo-incorrecto`

```bash
git checkout develop
git pull origin develop
git checkout -b bugfix/calculo-incorrecto

# Abrir un PR: bugfix/calculo-incorrecto → develop
```

---

### Diagrama del Flujo de Ramas

```
main ─────────────────────────────────────────────────► (producción)
  │                    ▲                  ▲
  │              release/x.x.x       hotfix/xxx
  │                    │                  │
develop ──────────────┼──────────────────┼──────────────►
  ▲        ▲          │                  │
  │        │          │                  │
feature/  bugfix/   (se crean desde develop)
  xxx       xxx
```

---

### Herramientas para GitFlow

**No se requiere ningún plugin ni herramienta externa.** Este repositorio utiliza comandos `git` nativos y GitHub Actions para toda la automatización de GitFlow:

| Mecanismo | Propósito |
|-----------|-----------|
| Workflow `init.yml` | Crea `develop` desde `main` y elimina ramas obsoletas (ejecutar una vez via **Actions → Init**) |
| Workflow `lint.yml` | Valida los títulos de PR (Conventional Commits) y los nombres de ramas en cada PR |
| `ci.yml` / `release.yml` | Ejecuta tests y publica releases |

Si prefieres herramientas locales para agilizar la gestión de ramas, [**git-flow AVH Edition**](https://github.com/petervanderdoes/gitflow-avh) es la opción recomendada — está mantenida activamente y envuelve los mismos comandos `git` nativos documentados arriba:

```bash
# Instalar (macOS)
brew install git-flow-avh

# Instalar (Ubuntu/Debian)
apt-get install git-flow

# Inicializar en el repositorio (acepta todos los valores por defecto)
git flow init -d

# Ejemplos
git flow feature start mi-funcionalidad   # → feature/mi-funcionalidad desde develop
git flow feature finish mi-funcionalidad  # → fusiona de vuelta en develop
git flow hotfix start 1.0.1               # → hotfix/1.0.1 desde main
```

> **Recomendación:** Usa `git` nativo + los workflows de GitHub Actions para el trabajo diario. El plugin AVH es un envoltorio de conveniencia — no aporta ninguna capacidad que los comandos nativos documentados no proporcionen ya.

---

### Nomenclatura de Versiones

Seguimos el **Versionado Semántico** ([semver.org](https://semver.org)):

```
MAYOR.MENOR.PARCHE
  │      │     └── Correcciones de bugs / hotfixes
  │      └──────── Nuevas funcionalidades compatibles con versiones anteriores
  └─────────────── Cambios que rompen la compatibilidad
```

---

### Convención de Mensajes de Commit

Seguimos la especificación **Conventional Commits** ([conventionalcommits.org](https://www.conventionalcommits.org)):

```
<tipo>(<alcance>): <descripción corta>

[cuerpo opcional]

[pie de página opcional]
```

#### Tipos

| Tipo       | Descripción                                           |
|------------|-------------------------------------------------------|
| `feat`     | Una nueva funcionalidad                               |
| `fix`      | Una corrección de bug                                 |
| `docs`     | Solo cambios en documentación                         |
| `style`    | Formato de código (sin cambios en la lógica)          |
| `refactor` | Reestructuración de código (sin funcionalidad ni fix) |
| `test`     | Agregar o actualizar tests                            |
| `chore`    | Proceso de build, actualizaciones de dependencias, cambios de CI |
| `perf`     | Mejoras de rendimiento                                |
| `ci`       | Cambios en la configuración de CI/CD                  |

#### Ejemplos

```
feat(auth): agregar autenticación basada en JWT
fix(monitor): resolver puntero nulo al iniciar
docs(contributing): agregar sección de estrategia de ramas
chore(deps): actualizar dependencias a las últimas versiones
```

---

### Guías para Pull Requests

1. **Un PR por funcionalidad/corrección** — mantén los PRs enfocados y pequeños.
2. **Completa la plantilla del PR** — describe el cambio, cómo probarlo y referencia los issues relacionados.
3. **Requiere al menos 1 aprobación** antes de hacer el merge.
4. **Todos los checks de CI deben pasar** antes del merge.
5. **Squash de commits** al fusionar ramas de funcionalidades en `develop` para mantener un historial limpio.
6. **Sin force pushes** en `main` ni en `develop`.

---

### Primeros Pasos

Antes de programar con asistentes de IA, lee primero [AGENTS.md](AGENTS.md) para reglas específicas del repositorio.

```bash
# 1. Haz un fork del repositorio (si eres colaborador externo)
# 2. Clona tu fork
git clone https://github.com/<tu-usuario>/Monitor.git
cd Monitor

# 3. Agrega el remote upstream
git remote add upstream https://github.com/careb36/Monitor.git

# 4. Siempre parte desde un develop actualizado
git fetch upstream
git checkout develop
git merge upstream/develop

# 5. Crea tu rama siguiendo las convenciones de nomenclatura anteriores
git checkout -b feature/mi-increible-funcionalidad

# 6. Haz tus cambios, haz commit y push
git add .
git commit -m "feat(alcance): descripción corta"
git push origin feature/mi-increible-funcionalidad

# 7. Abre un Pull Request hacia develop
```

---

### Código de Conducta

Por favor sé respetuoso/a y constructivo/a en todas las interacciones. Consulta [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) si está presente, o sigue el [Contributor Covenant](https://www.contributor-covenant.org).
