#!/usr/bin/env bash
set -euo pipefail

BACKEND_USER="${KAFKA_BACKEND_USERNAME:-backend-user}"
BACKEND_PASS="${KAFKA_BACKEND_PASSWORD:-backend-password}"
CONNECT_USER="${KAFKA_CONNECT_USERNAME:-connect-user}"
CONNECT_PASS="${KAFKA_CONNECT_PASSWORD:-connect-password}"

# This script intentionally bootstraps SCRAM credentials against ZooKeeper while
# the stack is still in PLAINTEXT mode to avoid circular auth bootstrapping.
for USERNAME in "$BACKEND_USER" "$CONNECT_USER"; do
  PASSWORD="$BACKEND_PASS"
  if [[ "$USERNAME" == "$CONNECT_USER" ]]; then
    PASSWORD="$CONNECT_PASS"
  fi

  echo "Creating SCRAM credentials for user: $USERNAME"
  docker compose exec -T kafka kafka-configs \
    --zookeeper zookeeper:2181 \
    --alter \
    --add-config "SCRAM-SHA-512=[iterations=4096,password=${PASSWORD}]" \
    --entity-type users \
    --entity-name "$USERNAME"
done

echo "SCRAM users created. Next step: restart with docker-compose.secure.yml"
