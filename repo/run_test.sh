#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Backend (Maven verify)"
(cd backend && ./mvnw -B verify)

echo "==> Frontend (Karma ChromeHeadless)"
(cd frontend && npm ci && npm run test:ci)

echo "All tests passed."
