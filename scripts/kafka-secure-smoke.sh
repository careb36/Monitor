#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

services=(kafka kafka-connect backend)
failed=0

echo "[1/4] Checking container status"
for svc in "${services[@]}"; do
  if ! docker compose ps "$svc" >/dev/null 2>&1; then
    echo "  [FAIL] service not found: $svc"
    failed=1
    continue
  fi

  status="$(docker compose ps --format json "$svc" | tr -d '\n' | sed 's/}\s*{/}\n{/g' | grep -m1 '"Service":"'"$svc"'"' || true)"
  if [[ -z "$status" ]]; then
    echo "  [FAIL] unable to read status: $svc"
    failed=1
    continue
  fi

  if echo "$status" | grep -q '"State":"running"'; then
    echo "  [OK] $svc running"
  else
    echo "  [FAIL] $svc not running"
    failed=1
  fi
done

echo "[2/4] Verifying secure env in backend"
backend_env="$(docker compose exec -T backend /bin/sh -lc 'env' 2>/dev/null || true)"
if echo "$backend_env" | grep -q '^KAFKA_SECURITY_PROTOCOL=SASL_SSL$'; then
  echo "  [OK] backend KAFKA_SECURITY_PROTOCOL=SASL_SSL"
else
  echo "  [FAIL] backend missing KAFKA_SECURITY_PROTOCOL=SASL_SSL"
  failed=1
fi

echo "[3/4] Verifying secure env in kafka-connect"
connect_env="$(docker compose exec -T kafka-connect /bin/sh -lc 'env' 2>/dev/null || true)"
if echo "$connect_env" | grep -q '^CONNECT_SECURITY_PROTOCOL=SASL_SSL$'; then
  echo "  [OK] connect CONNECT_SECURITY_PROTOCOL=SASL_SSL"
else
  echo "  [FAIL] connect missing CONNECT_SECURITY_PROTOCOL=SASL_SSL"
  failed=1
fi

echo "[4/4] Scanning logs for obvious auth/ssl failures"
kafka_logs="$(docker compose logs --tail=200 kafka 2>/dev/null || true)"
connect_logs="$(docker compose logs --tail=200 kafka-connect 2>/dev/null || true)"
backend_logs="$(docker compose logs --tail=200 backend 2>/dev/null || true)"
combined_logs="$kafka_logs
$connect_logs
$backend_logs"

if echo "$combined_logs" | grep -Eiq 'SSLHandshakeException|SaslAuthenticationException|PKIX path building failed|No route to host|Connection refused'; then
  echo "  [WARN] suspicious errors detected in recent logs"
  echo "         inspect with: docker compose logs --tail=200 kafka kafka-connect backend"
  failed=1
else
  echo "  [OK] no obvious TLS/SASL errors in recent logs"
fi

if [[ "$failed" -ne 0 ]]; then
  echo "Secure smoke test FAILED"
  exit 1
fi

echo "Secure smoke test PASSED"
