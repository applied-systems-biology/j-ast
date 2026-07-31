#!/bin/bash
set -euo pipefail

echo "[run-server] Building j-ast-backend..."
mvn -pl backend -am install -DskipTests -q

echo "[run-server] Starting server..."
mvn -pl backend spring-boot:run
