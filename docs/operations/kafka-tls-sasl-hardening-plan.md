# Kafka TLS/SASL Hardening Plan (Hallazgo #12)

Status: Completed (phase closed on 2026-04-02)

## Objective
Migrate local and production Kafka traffic from PLAINTEXT to authenticated and encrypted transport, without breaking Debezium and Spring Boot consumers.

## Completion Notes
- Secure rollout automation validated end-to-end.
- Operational evidence attached during release promotion workflow.
- Phase9 stabilization applied for startup readiness and TLS hostname compatibility.

## Scope
- Service: Kafka broker in docker-compose stack
- Clients: Debezium Kafka Connect, Spring Boot backend, optional local CLI tools
- Security target: TLS encryption + SASL/SCRAM authentication

## Non-goals
- No direct cutover in this phase 3 patch set
- No certificate authority automation in this document

Historical note:
- This document is retained as planning traceability artifact.
- Current run procedures are maintained in `kafka-sasl-ssl-quickstart.md` and related operations docs.

## Current risk
Current stack uses PLAINTEXT listeners and no SASL authentication. Any actor with network access can read or inject messages.

## Proposed rollout
1. Add secure listener profile in compose
- Keep existing PLAINTEXT listener for migration window
- Add parallel SASL_SSL listener and broker certificates

2. Provision credentials and trust material
- Generate broker keystore and truststore
- Generate client truststore for backend and Debezium
- Configure SCRAM users per client

3. Migrate clients one by one
- Debezium connect worker: set SASL/SCRAM and truststore
- Spring Boot app: set spring.kafka properties for SASL_SSL
- Validate topic read/write and connector offsets

4. Remove PLAINTEXT listener
- Remove legacy listener from advertised listeners
- Keep only SASL_SSL after all clients pass health checks

## Required environment variables
- KAFKA_SECURITY_MODE=sasl_ssl
- KAFKA_SASL_MECHANISM=SCRAM-SHA-512
- KAFKA_USERNAME
- KAFKA_PASSWORD
- KAFKA_TRUSTSTORE_PATH
- KAFKA_TRUSTSTORE_PASSWORD
- KAFKA_KEYSTORE_PATH
- KAFKA_KEYSTORE_PASSWORD

## Validation checklist
- Unauthorized consumer fails to connect
- Authorized producer and consumer can exchange messages
- Debezium connector remains healthy and advances offsets
- Backend starts cleanly and receives CDC events
- Packet capture does not expose plaintext payload

## Rollback plan
- Re-enable PLAINTEXT listener profile
- Revert client security properties to previous values
- Restart broker, connect, and backend in sequence

## Ownership
- Infra: compose and certificate handling
- Backend: Spring Kafka client configuration
- Data pipeline: Debezium configuration migration
