# Contributing to Monitor

🇬🇧 [English](#english) | 🇪🇸 [Español](#español)

---

<a id="english"></a>
## 🇬🇧 English

### Executive Summary

This repository follows a controlled contribution model based on:

- **GitFlow** for branch lifecycle and release discipline
- **Conventional Commits** for commit and PR title consistency
- **mandatory CI validation** before merge
- **protected branches** for `main` and `develop`

Use this document to understand how changes are proposed, reviewed, validated, and merged.

### Before You Start

Before implementation work:

- read [README.md](README.md) for the project overview
- read [docs/README.md](docs/README.md) for architecture, operations, ADRs, and diagrams
- read [AGENTS.md](AGENTS.md) first if AI coding assistants are involved

### Contribution Workflow

1. start from an up-to-date base branch
2. create a branch using the correct GitFlow prefix
3. implement and validate your changes locally
4. commit using Conventional Commits
5. push your branch and open a Pull Request
6. wait for CI + review approval
7. merge according to branch policy

### Branching Strategy: GitFlow

#### Main branches

| Branch | Purpose | Direct pushes |
|---|---|---|
| `main` | production-ready code | ❌ never |
| `develop` | integration branch for ongoing work | ❌ never |

Both branches are protected. All changes go through Pull Requests.

#### Working branches

| Branch type | Purpose | Source | Target | Example |
|---|---|---|---|---|
| `feature/*` | new features and improvements | `develop` | `develop` | `feature/live-alert-banner` |
| `bugfix/*` | non-critical fixes | `develop` | `develop` | `bugfix/polling-timeout` |
| `release/*` | release preparation | `develop` | `main` and `develop` | `release/1.2.0` |
| `hotfix/*` | urgent production fixes | `main` | `main` and `develop` | `hotfix/sse-reconnect` |

### Standard Branch Commands

#### Feature or bugfix

```bash
git checkout develop
git pull origin develop
git checkout -b feature/my-new-feature
# or
git checkout -b bugfix/my-fix
```

#### Release

```bash
git checkout develop
git pull origin develop
git checkout -b release/1.2.0
```

#### Hotfix

```bash
git checkout main
git pull origin main
git checkout -b hotfix/critical-fix
```

### Branch Flow Diagram

```text
main ───────────────────────────────────────────────► production
  │                    ▲                 ▲
  │              release/x.x.x      hotfix/xxx
  │                    │                 │
develop ───────────────┼─────────────────┼──────────► integration
  ▲         ▲          │                 │
  │         │          │                 │
feature/*  bugfix/*    └──── merge back ─┘
```

### Tooling and Automation

No external GitFlow plugin is required.
This repository uses native `git` plus GitHub Actions workflows.

| Mechanism | Purpose |
|---|---|
| `.github/workflows/init.yml` | initialize `develop` from `main` |
| `.github/workflows/lint.yml` | validate PR titles and branch names |
| `.github/workflows/ci.yml` | build and test feature / develop work |
| `.github/workflows/release.yml` | release / hotfix validation and tagging |
| `.github/workflows/deploy.yml` | production build and deploy path |

If you prefer local helpers, `git-flow AVH Edition` is optional, not required.

### Versioning

This project follows **Semantic Versioning**:

```text
MAJOR.MINOR.PATCH
```

- `MAJOR` for breaking changes
- `MINOR` for backward-compatible features
- `PATCH` for bug fixes and hotfixes

### Commit Convention

This repository follows **Conventional Commits**.

```text
<type>(<scope>): <short description>
```

#### Common types

| Type | Meaning |
|---|---|
| `feat` | new feature |
| `fix` | bug fix |
| `docs` | documentation only |
| `style` | formatting without logic change |
| `refactor` | restructuring without feature or bug fix |
| `test` | test changes |
| `chore` | build, dependency, or maintenance work |
| `perf` | performance improvement |
| `ci` | CI/CD change |

#### Examples

```text
feat(sse): add connection heartbeat indicator
fix(runtime): restore backend startup after Boot upgrade
docs(architecture): add C4 and runbook documentation
chore(deps): update frontend tooling
```

### Pull Request Policy

Every Pull Request should be:

- focused on one feature, fix, or documentation topic
- linked to the correct target branch
- validated locally before submission
- described using the PR template
- approved before merge

#### Merge expectations

- at least 1 approval is required
- all CI checks must pass
- no force pushes on `main` or `develop`
- squash merge is preferred for feature work into `develop`

### Local Validation Expectations

#### Backend

```bash
mvn --batch-mode clean verify
```

#### Frontend

```bash
cd frontend
npm install
npm run lint
npm run build
```

### Getting Started

```bash
# 1. Clone your fork or the repository
git clone https://github.com/<your-username>/Monitor.git
cd Monitor

# 2. Add upstream if needed
git remote add upstream https://github.com/careb36/Monitor.git

# 3. Sync your base branch
git fetch upstream
git checkout develop
git merge upstream/develop

# 4. Create a working branch
git checkout -b feature/my-awesome-feature

# 5. Implement and validate changes
# 6. Commit using Conventional Commits
git add .
git commit -m "feat(scope): short description"

# 7. Push and open a Pull Request
git push origin feature/my-awesome-feature
```

### Conduct and Review Culture

Be respectful, specific, and constructive in reviews and discussions.
If a dedicated `CODE_OF_CONDUCT.md` is added later, it becomes the governing reference.

---

<a id="español"></a>
## 🇪🇸 Español

### Resumen Ejecutivo

Este repositorio sigue un modelo de contribución controlado basado en:

- **GitFlow** para el ciclo de vida de ramas y disciplina de releases
- **Conventional Commits** para consistencia en commits y títulos de PR
- **validación obligatoria en CI** antes del merge
- **ramas protegidas** para `main` y `develop`

Usa este documento para entender cómo se proponen, revisan, validan y fusionan los cambios.

### Antes de Empezar

Antes de implementar cambios:

- revisa [README.md](README.md) para la visión general del proyecto
- revisa [docs/README.md](docs/README.md) para arquitectura, operaciones, ADRs y diagramas
- lee [AGENTS.md](AGENTS.md) primero si intervienen asistentes de IA

### Flujo de Contribución

1. parte desde una rama base actualizada
2. crea una rama con el prefijo GitFlow correcto
3. implementa y valida localmente tus cambios
4. haz commit usando Conventional Commits
5. publica tu rama y abre un Pull Request
6. espera CI y aprobación de revisión
7. fusiona según la política de ramas

### Estrategia de Ramas: GitFlow

#### Ramas principales

| Rama | Propósito | Push directo |
|---|---|---|
| `main` | código listo para producción | ❌ nunca |
| `develop` | rama de integración para trabajo en curso | ❌ nunca |

Ambas ramas están protegidas. Todos los cambios pasan por Pull Requests.

#### Ramas de trabajo

| Tipo de rama | Propósito | Origen | Destino | Ejemplo |
|---|---|---|---|---|
| `feature/*` | nuevas funcionalidades y mejoras | `develop` | `develop` | `feature/live-alert-banner` |
| `bugfix/*` | correcciones no críticas | `develop` | `develop` | `bugfix/polling-timeout` |
| `release/*` | preparación de release | `develop` | `main` y `develop` | `release/1.2.0` |
| `hotfix/*` | correcciones urgentes de producción | `main` | `main` y `develop` | `hotfix/sse-reconnect` |

### Comandos Estándar de Ramas

#### Feature o bugfix

```bash
git checkout develop
git pull origin develop
git checkout -b feature/my-new-feature
# o
git checkout -b bugfix/my-fix
```

#### Release

```bash
git checkout develop
git pull origin develop
git checkout -b release/1.2.0
```

#### Hotfix

```bash
git checkout main
git pull origin main
git checkout -b hotfix/critical-fix
```

### Diagrama del Flujo de Ramas

```text
main ───────────────────────────────────────────────► produccion
  │                    ▲                 ▲
  │              release/x.x.x      hotfix/xxx
  │                    │                 │
develop ───────────────┼─────────────────┼──────────► integracion
  ▲         ▲          │                 │
  │         │          │                 │
feature/*  bugfix/*    └──── vuelve via merge ──────┘
```

### Tooling y Automatización

No se requiere ningún plugin externo de GitFlow.
Este repositorio usa `git` nativo más workflows de GitHub Actions.

| Mecanismo | Propósito |
|---|---|
| `.github/workflows/init.yml` | inicializar `develop` desde `main` |
| `.github/workflows/lint.yml` | validar títulos de PR y nombres de rama |
| `.github/workflows/ci.yml` | compilar y probar trabajo de feature / develop |
| `.github/workflows/release.yml` | validación y etiquetado de release / hotfix |
| `.github/workflows/deploy.yml` | build y ruta de despliegue a producción |

Si prefieres ayudantes locales, `git-flow AVH Edition` es opcional, no obligatorio.

### Versionado

Este proyecto sigue **Versionado Semántico**:

```text
MAYOR.MENOR.PARCHE
```

- `MAYOR` para cambios incompatibles
- `MENOR` para funcionalidades compatibles hacia atrás
- `PARCHE` para correcciones y hotfixes

### Convención de Commits

Este repositorio sigue **Conventional Commits**.

```text
<tipo>(<alcance>): <descripcion corta>
```

#### Tipos comunes

| Tipo | Significado |
|---|---|
| `feat` | nueva funcionalidad |
| `fix` | corrección de bug |
| `docs` | solo documentación |
| `style` | formato sin cambio lógico |
| `refactor` | reestructuración sin feature ni fix |
| `test` | cambios en pruebas |
| `chore` | build, dependencias o mantenimiento |
| `perf` | mejora de rendimiento |
| `ci` | cambio en CI/CD |

#### Ejemplos

```text
feat(sse): add connection heartbeat indicator
fix(runtime): restore backend startup after Boot upgrade
docs(architecture): add C4 and runbook documentation
chore(deps): update frontend tooling
```

### Política de Pull Requests

Todo Pull Request debe ser:

- enfocado en una sola funcionalidad, fix o tema documental
- dirigido a la rama destino correcta
- validado localmente antes de enviarse
- descrito usando la plantilla de PR
- aprobado antes del merge

#### Expectativas de merge

- se requiere al menos 1 aprobación
- todos los checks de CI deben pasar
- no se permiten force pushes en `main` ni `develop`
- se prefiere squash merge para trabajo de feature hacia `develop`

### Validación Local Esperada

#### Backend

```bash
mvn --batch-mode clean verify
```

#### Frontend

```bash
cd frontend
npm install
npm run lint
npm run build
```

### Primeros Pasos

```bash
# 1. Clona tu fork o el repositorio
git clone https://github.com/<your-username>/Monitor.git
cd Monitor

# 2. Agrega upstream si hace falta
git remote add upstream https://github.com/careb36/Monitor.git

# 3. Sincroniza tu rama base
git fetch upstream
git checkout develop
git merge upstream/develop

# 4. Crea una rama de trabajo
git checkout -b feature/my-awesome-feature

# 5. Implementa y valida cambios
# 6. Haz commit usando Conventional Commits
git add .
git commit -m "feat(alcance): descripcion corta"

# 7. Publica la rama y abre un Pull Request
git push origin feature/my-awesome-feature
```

### Cultura de Revisión y Conducta

Mantén interacciones respetuosas, específicas y constructivas en revisiones y discusiones.
Si se añade más adelante un `CODE_OF_CONDUCT.md`, ese archivo se convertirá en la referencia principal.
