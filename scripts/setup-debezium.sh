#!/usr/bin/env bash
CONNECT_URL="http://localhost:8083/connectors"
CONNECTOR_NAME="oracle-log-traza-connector"
JSON_PATH="./docker/debezium/connector-log-traza.json"
echo "Registering connector..."
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" "$CONNECT_URL" -d @"$JSON_PATH"
