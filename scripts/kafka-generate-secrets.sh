#!/usr/bin/env bash
set -euo pipefail

SECRETS_DIR="docker/kafka/secrets"
DNAME="CN=monitor-kafka, OU=Monitor, O=Monitor, L=Local, ST=Local, C=US"
SUBJECT_ALT_NAMES="SAN=dns:kafka,dns:localhost,dns:monitor-kafka,ip:127.0.0.1"
STOREPASS="${KAFKA_KEYSTORE_PASSWORD:-changeit}"
TRUSTPASS="${KAFKA_TRUSTSTORE_PASSWORD:-changeit}"
VALIDITY_DAYS="${KAFKA_CERT_VALIDITY_DAYS:-365}"
BROKER_USER="${KAFKA_BROKER_USERNAME:-broker-user}"
BROKER_PASS="${KAFKA_BROKER_PASSWORD:-broker-password}"

mkdir -p "$SECRETS_DIR"

# Clean previous artifacts to avoid stale/corrupt stores in repeated local runs.
rm -f "$SECRETS_DIR"/*.jks "$SECRETS_DIR"/*.crt "$SECRETS_DIR"/*.csr "$SECRETS_DIR"/*.properties "$SECRETS_DIR"/*jaas.conf || true

echo "[1/5] Generating broker keystore"
keytool -genkeypair \
  -alias kafka-broker \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -dname "$DNAME" \
  -ext "$SUBJECT_ALT_NAMES" \
  -keystore "$SECRETS_DIR/kafka.keystore.jks" \
  -storepass "$STOREPASS" \
  -keypass "$STOREPASS" \
  -storetype JKS

echo "[2/5] Exporting broker certificate"
keytool -exportcert \
  -alias kafka-broker \
  -keystore "$SECRETS_DIR/kafka.keystore.jks" \
  -storepass "$STOREPASS" \
  -rfc \
  -file "$SECRETS_DIR/kafka-broker.crt"

echo "[3/5] Creating shared truststore"
keytool -importcert \
  -alias kafka-broker \
  -file "$SECRETS_DIR/kafka-broker.crt" \
  -keystore "$SECRETS_DIR/kafka.truststore.jks" \
  -storepass "$TRUSTPASS" \
  -noprompt

echo "[4/5] Creating client truststores"
cp "$SECRETS_DIR/kafka.truststore.jks" "$SECRETS_DIR/connect.truststore.jks"
cp "$SECRETS_DIR/kafka.truststore.jks" "$SECRETS_DIR/backend.truststore.jks"

echo "[5/5] Writing credential helper files"
printf '%s\n' "$STOREPASS" > "$SECRETS_DIR/kafka_keystore_creds"
printf '%s\n' "$STOREPASS" > "$SECRETS_DIR/kafka_sslkey_creds"
printf '%s\n' "$TRUSTPASS" > "$SECRETS_DIR/kafka_truststore_creds"

chmod 600 \
  "$SECRETS_DIR/kafka_keystore_creds" \
  "$SECRETS_DIR/kafka_sslkey_creds" \
  "$SECRETS_DIR/kafka_truststore_creds"

cat > "$SECRETS_DIR/kafka_server_jaas.conf" <<EOF
KafkaServer {
  org.apache.kafka.common.security.scram.ScramLoginModule required
  username="${BROKER_USER}"
  password="${BROKER_PASS}";
};
EOF

cat > "$SECRETS_DIR/kafka_admin_client.properties" <<EOF
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="${BROKER_USER}" password="${BROKER_PASS}";
ssl.truststore.location=/etc/kafka/secrets/kafka.truststore.jks
ssl.truststore.password=${TRUSTPASS}
ssl.endpoint.identification.algorithm=
EOF

chmod 600 "$SECRETS_DIR/kafka_server_jaas.conf" "$SECRETS_DIR/kafka_admin_client.properties"

echo "Kafka TLS artifacts generated under $SECRETS_DIR"
echo "Remember to export KAFKA_CONNECT_TRUSTSTORE_PASSWORD and KAFKA_BACKEND_TRUSTSTORE_PASSWORD to match $TRUSTPASS"
