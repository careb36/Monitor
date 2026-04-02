#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SECRETS_DIR="$ROOT_DIR/docker/kafka/secrets"

required_env=(
  KAFKA_BROKER_USERNAME
  KAFKA_BROKER_PASSWORD
  KAFKA_BACKEND_USERNAME
  KAFKA_BACKEND_PASSWORD
  KAFKA_CONNECT_USERNAME
  KAFKA_CONNECT_PASSWORD
  KAFKA_CONNECT_TRUSTSTORE_PASSWORD
  KAFKA_BACKEND_TRUSTSTORE_PASSWORD
)

required_files=(
  kafka.keystore.jks
  kafka.truststore.jks
  connect.truststore.jks
  backend.truststore.jks
  kafka_server_jaas.conf
  kafka_admin_client.properties
  kafka_keystore_creds
  kafka_sslkey_creds
  kafka_truststore_creds
)

missing=0

echo "Checking required environment variables..."
for var in "${required_env[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "  [MISSING] $var"
    missing=1
  else
    echo "  [OK] $var"
  fi
done

echo "Checking required secret files under docker/kafka/secrets/..."
for file in "${required_files[@]}"; do
  if [[ ! -f "$SECRETS_DIR/$file" ]]; then
    echo "  [MISSING] docker/kafka/secrets/$file"
    missing=1
  else
    echo "  [OK] docker/kafka/secrets/$file"
  fi
done

if [[ $missing -ne 0 ]]; then
  echo "Preflight failed. Fill missing env vars/files before secure mode activation."
  exit 1
fi

echo "Preflight passed. Secure mode prerequisites are complete."
