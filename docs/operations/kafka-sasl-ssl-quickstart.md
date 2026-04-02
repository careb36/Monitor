# Kafka SASL/SSL Quickstart

## Purpose
Enable encrypted and authenticated Kafka traffic using `docker-compose.secure.yml` on top of the default stack.

## 1) Prepare secrets
Generate truststores/keystores and credential files automatically:

```bash
./scripts/kafka-generate-secrets.sh
```

Artifacts are written under:
- `docker/kafka/secrets/`

See required filenames in:
- `docker/kafka/secrets/README.md`

## 2) Export required environment variables
Example:

```bash
export KAFKA_CONNECT_USERNAME=connect-user
export KAFKA_CONNECT_PASSWORD=connect-password
export KAFKA_CONNECT_TRUSTSTORE_PASSWORD=changeit

export KAFKA_BACKEND_USERNAME=backend-user
export KAFKA_BACKEND_PASSWORD=backend-password
export KAFKA_BACKEND_TRUSTSTORE_PASSWORD=changeit
```

## 3) Start secure stack

```bash
docker compose up -d kafka zookeeper
./scripts/kafka-bootstrap-scram-users.sh
docker compose -f docker-compose.yml -f docker-compose.secure.yml up -d
```

Or run the full flow in one command:

```bash
./scripts/kafka-enable-secure-mode.sh
```

## 4) Verify secure mode
- Kafka broker listener map includes `SASL_SSL`
- Backend starts with `KAFKA_SECURITY_PROTOCOL=SASL_SSL`
- Debezium connect worker starts with SASL settings

## 5) Rollback to plaintext

```bash
docker compose -f docker-compose.yml up -d
```

This removes secure override and returns to base plaintext mode.
