#!/usr/bin/env bash
# Run backend Maven verify in a JDK 17 container.
# - Persists Maven dependencies in a named volume (avoids re-downloading every run).
# - Mounts the Docker socket so Testcontainers can start PostgreSQL for integration tests.
#
# Usage (from repo/):  ./scripts/mvn-verify-docker.sh
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
M2_VOLUME="city-bus-maven-cache"

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "${REPO_ROOT}:/work" \
  -v "${M2_VOLUME}:/root/.m2" \
  -v "/var/run/docker.sock:/var/run/docker.sock" \
  -w /work/backend \
  eclipse-temurin:17-jdk \
  ./mvnw -B verify "$@"
