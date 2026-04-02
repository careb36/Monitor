# Kafka Security Secrets (Local)

This directory stores local TLS/SASL materials used by `docker-compose.secure.yml`.

Do not commit private keys, truststores, or credentials.

Expected files:
- kafka.keystore.jks
- kafka.truststore.jks
- connect.truststore.jks
- backend.truststore.jks
- kafka_keystore_creds
- kafka_sslkey_creds
- kafka_truststore_creds

These files are ignored by `.gitignore` (except this README and `.gitkeep`).
