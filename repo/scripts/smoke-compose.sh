#!/usr/bin/env bash
# Optional Compose smoke: requires curl. Run after: docker compose up -d --build
set -euo pipefail
BASE="${1:-http://localhost}"
echo "Checking ${BASE}/actuator/health ..."
curl -fsS "${BASE}/actuator/health" | head -c 200
echo
echo "Login and call ${BASE}/api/v1/ping with Bearer token ..."
RESP=$(curl -fsS -X POST "${BASE}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"ChangeMe123!"}')
TOKEN=$(echo "$RESP" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [[ -z "${TOKEN}" ]]; then
  echo "Failed to parse accessToken from login response" >&2
  exit 1
fi
curl -fsS -H "Authorization: Bearer ${TOKEN}" "${BASE}/api/v1/ping" | head -c 200
echo
echo "Smoke checks OK."
