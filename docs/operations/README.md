# Operations Documentation

**Runbooks and Operational Guidance / Runbooks y Guia Operativa**

[English](#english) | [Espanol](#espanol)

---

<a id="english"></a>
## English

### Executive Summary

This folder is the operational entry point for validating and troubleshooting `Monitor`.
It is designed to answer one question quickly: **is the platform healthy, observable, and connected end to end?**

### Purpose

This folder contains operational documentation for the current `Monitor` implementation.
It is intended for developers, operators, and support engineers who need repeatable procedures for startup, smoke validation, and troubleshooting.

### Index

- `monitor-operations-runbook.md` - day-to-day operational procedures, smoke checks, and incident-oriented troubleshooting
- `kafka-sasl-ssl-quickstart.md` - secure Kafka enablement (TLS/SASL) with bootstrap scripts
- `kafka-sasl-ssl-troubleshooting.md` - failure patterns and diagnostics for Kafka secure mode
- `kafka-sasl-ssl-acceptance-checklist.md` - release checklist to close secure Kafka rollout

### Scope

The operations set covers:

- local startup and validation
- backend/frontend smoke checks
- SSE connection health and heartbeats
- Kafka / Debezium ingestion checks
- polling behavior checks
- CRITICAL email notification checks

---

<a id="espanol"></a>
## Espanol

### Resumen Ejecutivo

Esta carpeta es el punto de entrada operativo para validar y diagnosticar `Monitor`.
Esta pensada para responder rapidamente una pregunta: **�la plataforma esta sana, observable y conectada de extremo a extremo?**

### Proposito

Esta carpeta contiene documentacion operativa para la implementacion actual de `Monitor`.
Esta pensada para desarrolladores, operadores y personal de soporte que necesiten procedimientos repetibles para arranque, validacion rapida y troubleshooting.

### Indice

- `monitor-operations-runbook.md` - procedimientos operativos diarios, smoke checks y troubleshooting orientado a incidentes
- `kafka-sasl-ssl-quickstart.md` - habilitacion de Kafka seguro (TLS/SASL) con scripts de bootstrap
- `kafka-sasl-ssl-troubleshooting.md` - guia de diagnostico para fallas de Kafka en modo seguro
- `kafka-sasl-ssl-acceptance-checklist.md` - checklist de aceptacion para cierre operativo de Kafka seguro

### Alcance

El set operativo cubre:

- arranque y validacion local
- smoke checks de backend y frontend
- salud de conexion SSE y heartbeats
- verificaciones de ingestion Kafka / Debezium
- comportamiento del polling
- verificaciones del flujo de correo para eventos CRITICAL

