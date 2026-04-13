#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Backend (Maven verify via Docker)"
bash "$ROOT/scripts/mvn-verify-docker.sh"

echo "==> Frontend (Karma ChromeHeadless)"
if [ -z "${CHROME_BIN:-}" ]; then
  for candidate in \
    "/c/Program Files/Google/Chrome/Application/chrome.exe" \
    "/c/Program Files (x86)/Google/Chrome/Application/chrome.exe" \
    "$(command -v google-chrome 2>/dev/null || true)" \
    "$(command -v chromium-browser 2>/dev/null || true)" \
    "$(command -v chromium 2>/dev/null || true)"; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then
      export CHROME_BIN="$candidate"
      break
    fi
  done
fi
(cd frontend && npm ci && npm run test:ci)

echo "All tests passed."
