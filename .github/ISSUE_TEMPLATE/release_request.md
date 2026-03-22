---
name: Release Request
about: Request a new release version
title: "release: v"
labels: release
assignees: ""
---

## Release Version

<!-- e.g., v1.2.0 -->

## Included Features / Fixes

<!-- List the features and fixes included in this release -->

- 
- 

## Pre-release Checklist

- [ ] All feature branches merged into `develop`
- [ ] `release/<version>` branch created from `develop`
- [ ] All tests pass on the release branch
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] PR opened: `release/<version>` → `main`
- [ ] PR opened: `release/<version>` → `develop`
- [ ] Version tag created on `main` after merge
