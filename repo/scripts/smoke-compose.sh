#!/usr/bin/env bash
# Optional Compose smoke: requires curl. Run after: docker compose up -d --build
set -euo pipefail
BASE="${1:-http://localhost}"
echo "Checking ${BASE}/actuator/health ..."
curl -fsS "${BASE}/actuator/health" | head -c 200
echo
echo "Checking ${BASE}/api/v1/ping ..."
curl -fsS "${BASE}/api/v1/ping" | head -c 200
echo
echo "Smoke checks OK."
