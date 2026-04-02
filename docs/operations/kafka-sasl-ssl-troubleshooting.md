# Kafka SASL/SSL Troubleshooting

## Common failure patterns

### 1) Authentication failed (SASL)
Symptoms:
- Backend or connect logs show authentication exceptions
- Kafka client cannot establish session

Checks:
- Verify usernames/passwords exported in environment
- Re-run SCRAM bootstrap script:
  - ./scripts/kafka-bootstrap-scram-users.sh
- Ensure secure overlay is active:
  - docker compose -f docker-compose.yml -f docker-compose.secure.yml ps

### 2) SSL handshake failure
Symptoms:
- javax.net.ssl.SSLHandshakeException
- PKIX path building failed

Checks:
- Verify truststore files exist under docker/kafka/secrets
- Verify truststore passwords match env values
- Regenerate artifacts if in doubt:
  - ./scripts/kafka-generate-secrets.sh

### 3) Broker starts but clients fail
Symptoms:
- Kafka service healthy, backend/connect unhealthy

Checks:
- Run preflight:
  - ./scripts/kafka-secure-preflight.sh
- Verify KAFKA_SECURITY_PROTOCOL is SASL_SSL in backend env
- Verify connect env includes CONNECT_SECURITY_PROTOCOL=SASL_SSL

### 4) Secure mode rollout stuck
Recovery path:
1. Roll back to plaintext:
   - docker compose -f docker-compose.yml up -d
2. Recreate credentials and secrets
3. Re-run secure activation flow

### 5) Broker unhealthy with ZooKeeper timeout during startup preflight
Symptoms:
- Kafka container repeatedly restarts or becomes unhealthy
- logs show timeout waiting for ZooKeeper while SASL is enabled

Checks:
- Verify secure overlay sets `ZOOKEEPER_SASL_ENABLED=false`
- Verify `KAFKA_OPTS` includes JAAS file and `-Dzookeeper.sasl.client=false`

Why:
- cp-kafka startup helper can run `cub zk-ready` with JAAS options unless ZooKeeper SASL is explicitly disabled for this path.

### 6) SSL handshake failed: No name matching kafka found
Symptoms:
- Kafka logs show `SSLHandshakeException` with hostname mismatch
- broker never reaches healthy state in secure overlay

Checks:
- Regenerate TLS materials with broker certificate SANs including `kafka`, `localhost`, and `monitor-kafka`
- Re-run `./scripts/kafka-generate-secrets.sh`
- Re-run full secure flow: `./scripts/kafka-enable-secure-mode.sh`

## Quick diagnostics
- Container state:
  - docker compose ps
- Backend logs:
  - docker compose logs --tail=200 backend
- Kafka connect logs:
  - docker compose logs --tail=200 kafka-connect
- Kafka logs:
  - docker compose logs --tail=200 kafka
