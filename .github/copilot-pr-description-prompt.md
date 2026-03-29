# Copilot PR Description Prompt

## PR Title ? REQUIRED FORMAT

The PR **title** MUST follow the **Conventional Commits** specification enforced by the `lint.yml` workflow.

```
<type>(<optional-scope>): <short description in lowercase>
```

### Allowed types

| Type       | When to use                                      |
|------------|--------------------------------------------------|
| `feat`     | New feature                                      |
| `fix`      | Bug fix                                          |
| `docs`     | Documentation only                               |
| `style`    | Formatting, no logic change                      |
| `refactor` | Code restructuring                               |
| `test`     | Adding or updating tests                         |
| `chore`    | Build, deps, CI changes                          |
| `perf`     | Performance improvements                         |
| `ci`       | CI/CD configuration changes                      |

### Valid examples

```
feat(auth): add JWT-based authentication
fix(monitor): resolve null pointer on startup
docs(contributing): update branching strategy
chore(deps): upgrade spring-boot to 3.3.0
test(polling): add unit tests for PollingService
ci(lint): add auto-fix for Copilot PR titles
```

### Invalid ? these will be rejected by the lint workflow

```
? Add authentication feature
? copilot: fix the thing
? Update something
? Fixed bug
```

---

## PR Body

Use the existing PULL_REQUEST_TEMPLATE.md sections:
- **Description**: one paragraph explaining what and why
- **Type of Change**: check the matching checkbox
- **Branch Checklist**: confirm branch naming and base branch
- **Testing**: describe what was tested and how

