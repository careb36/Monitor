#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f docker-compose.secure.yml ]]; then
  echo "docker-compose.secure.yml not found"
  exit 1
fi

echo "Step 1/4: generate TLS materials"
./scripts/kafka-generate-secrets.sh

echo "Step 2/4: start base stack (plaintext bootstrap)"
docker compose up -d kafka zookeeper

echo "Step 3/4: create SCRAM users"
./scripts/kafka-bootstrap-scram-users.sh

echo "Step 4/5: preflight check"
./scripts/kafka-secure-preflight.sh

echo "Step 5/5: apply secure override"
docker compose -f docker-compose.yml -f docker-compose.secure.yml up -d kafka kafka-connect backend

echo "Secure mode applied. Validate with: docker compose ps"
