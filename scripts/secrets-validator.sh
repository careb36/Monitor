#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# secrets-validator.sh — Pre-flight check for required environment variables.
# Referenced from: docs/reports/SECURITY-CONFIG.md §4 Secret Management
#
# Usage:
#   source .env          # load your local environment
#   ./scripts/secrets-validator.sh
# ---------------------------------------------------------------------------

set -euo pipefail

REQUIRED_VARS=(
  ORACLE_PASSWORD
  MONITOR_APP_PASSWORD
  MONITOR_PASSWORD
  MAIL_PASSWORD
)

WEAK_PATTERNS=(
  "changeme"
  "password"
  "123"
  "admin"
  "secret"
  "default"
  "changeme-in-production"
)

RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m'

errors=0
warnings=0

echo "═══════════════════════════════════════════════════"
echo "  Monitor — Secrets Pre-flight Validator"
echo "═══════════════════════════════════════════════════"
echo ""

# --- Check required variables exist and are non-empty ---
for var in "${REQUIRED_VARS[@]}"; do
  value="${!var:-}"
  if [[ -z "$value" ]]; then
    echo -e "  ${RED}✗ MISSING${NC}  $var is not set or empty"
    errors=$((errors + 1))
    continue
  fi

  # --- Check for weak/default values ---
  lower_value="${value,,}"
  for pattern in "${WEAK_PATTERNS[@]}"; do
    if [[ "$lower_value" == "$pattern" ]]; then
      echo -e "  ${YELLOW}⚠ WEAK${NC}    $var uses a default/weak value (\"$pattern\")"
      warnings=$((warnings + 1))
      break
    fi
  done
done

# --- Check .env is in .gitignore ---
if [[ -f ".gitignore" ]]; then
  if ! grep -qx '\.env' .gitignore 2>/dev/null && \
     ! grep -qx '.env' .gitignore 2>/dev/null; then
    echo -e "  ${YELLOW}⚠ GITIGNORE${NC}  .env is not listed in .gitignore"
    warnings=$((warnings + 1))
  fi
fi

echo ""
echo "───────────────────────────────────────────────────"

if [[ $errors -gt 0 ]]; then
  echo -e "  ${RED}FAILED${NC}: $errors missing variable(s), $warnings warning(s)"
  echo "  Fix the errors above before starting the application."
  echo ""
  exit 1
elif [[ $warnings -gt 0 ]]; then
  echo -e "  ${YELLOW}PASSED WITH WARNINGS${NC}: $warnings weak value(s) detected"
  echo "  Consider rotating secrets before deploying to production."
  echo ""
  exit 0
else
  echo -e "  ${GREEN}✓ ALL CHECKS PASSED${NC}: ${#REQUIRED_VARS[@]} secrets validated"
  echo ""
  exit 0
fi
