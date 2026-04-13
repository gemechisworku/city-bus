# Run backend Maven verify in a JDK 17 container.
# - Persists Maven dependencies in a named volume (avoids re-downloading every run).
# - Mounts the Docker socket so Testcontainers can start PostgreSQL for integration tests.
#
# Usage (from repo/):  .\scripts\mvn-verify-docker.ps1
# Extra args are passed to mvnw, e.g. .\scripts\mvn-verify-docker.ps1 -DskipTests

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$M2Volume = "city-bus-maven-cache"

docker run --rm `
  --add-host=host.docker.internal:host-gateway `
  -v "${RepoRoot}:/work" `
  -v "${M2Volume}:/root/.m2" `
  -v "//var/run/docker.sock:/var/run/docker.sock" `
  -w /work/backend `
  eclipse-temurin:17-jdk `
  ./mvnw -B verify @args
