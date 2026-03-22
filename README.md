# Monitor

A monitoring application built with Java.

## Branching Strategy

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

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## CI/CD

| Workflow      | Trigger                              | Purpose                  |
|---------------|--------------------------------------|--------------------------|
| CI – Feature  | Push/PR to `develop`, `feature/*`, `bugfix/*` | Build & test |
| CI – Release  | Push/PR on `release/*`, `hotfix/*`  | Build, test & tag        |
| CD – Deploy   | Push to `main`                       | Build & deploy           |
