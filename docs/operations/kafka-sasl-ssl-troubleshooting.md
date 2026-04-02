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

## Quick diagnostics
- Container state:
  - docker compose ps
- Backend logs:
  - docker compose logs --tail=200 backend
- Kafka connect logs:
  - docker compose logs --tail=200 kafka-connect
- Kafka logs:
  - docker compose logs --tail=200 kafka
