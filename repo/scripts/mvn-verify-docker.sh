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

if [ -e //./pipe/docker_engine ]; then
  DOCKER_SOCK_MOUNT="//./pipe/docker_engine://./pipe/docker_engine"
  DOCKER_HOST_ENV=(-e DOCKER_HOST=npipe:////./pipe/docker_engine)
else
  DOCKER_SOCK_MOUNT="/var/run/docker.sock:/var/run/docker.sock"
  DOCKER_HOST_ENV=()
fi

export MSYS_NO_PATHCONV=1

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "${REPO_ROOT}:/work" \
  -v "${M2_VOLUME}:/root/.m2" \
  -v "${DOCKER_SOCK_MOUNT}" \
  "${DOCKER_HOST_ENV[@]}" \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /work/backend \
  eclipse-temurin:17-jdk \
  ./mvnw -B verify "$@"
