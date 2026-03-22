# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- GitFlow branching strategy with `main`, `develop`, `feature/`, `release/`, `hotfix/`, and `bugfix/` branches
- GitHub Actions CI workflows for feature, develop, release, and hotfix branches
- GitHub Actions CD workflow for automated deployment from `main`
- Pull Request template following GitFlow conventions
- Issue templates: Bug Report, Feature Request, Release Request
- `CONTRIBUTING.md` with full GitFlow documentation and commit conventions
- Automatic version tagging for `hotfix/**` branches (reads version from `pom.xml`)
- Lint workflow to validate PR titles against Conventional Commits and enforce GitFlow branch naming
- Init workflow to create the `develop` branch from `main` via `workflow_dispatch`

---

<!-- 
Template for future releases:

## [x.y.z] – YYYY-MM-DD

### Added
- 

### Changed
- 

### Deprecated
- 

### Removed
- 

### Fixed
- 

### Security
- 
-->

[Unreleased]: https://github.com/careb36/Monitor/compare/main...develop
