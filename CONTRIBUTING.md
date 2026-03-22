# Contributing to Monitor

Thank you for your interest in contributing! This document describes our branching strategy and contribution workflow.

---

## Branching Strategy: GitFlow

We follow the **GitFlow** branching model, a well-established workflow that provides a robust framework for managing releases, features, and hotfixes.

### Main Branches

| Branch    | Purpose                                    | Direct pushes |
|-----------|--------------------------------------------|---------------|
| `main`    | Production-ready code. Every commit here is a release. | ❌ Never |
| `develop` | Integration branch. All features merge here first.     | ❌ Never |

Both `main` and `develop` are **protected branches**. All changes must go through a Pull Request.

---

### Supporting Branches

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

## Branch Flow Diagram

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

## Version Naming

We follow **Semantic Versioning** ([semver.org](https://semver.org)):

```
MAJOR.MINOR.PATCH
  │      │     └── Bug fixes / hotfixes
  │      └──────── New backward-compatible features
  └─────────────── Breaking changes
```

---

## Commit Message Convention

We follow the **Conventional Commits** specification ([conventionalcommits.org](https://www.conventionalcommits.org)):

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

### Types

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

### Examples

```
feat(auth): add JWT-based authentication
fix(monitor): resolve null pointer on startup
docs(contributing): add branching strategy section
chore(deps): update dependencies to latest versions
```

---

## Pull Request Guidelines

1. **One PR per feature/fix** — keep PRs focused and small.
2. **Fill in the PR template** — describe the change, how to test it, and reference related issues.
3. **Require at least 1 approval** before merging.
4. **All CI checks must pass** before merging.
5. **Squash commits** when merging feature branches to `develop` to keep history clean.
6. **No force pushes** on `main` or `develop`.

---

## Getting Started

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

## Code of Conduct

Please be respectful and constructive in all interactions. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) if present, or follow the [Contributor Covenant](https://www.contributor-covenant.org).
