#!/usr/bin/env bash
# Backup the PostgreSQL database from the running Compose stack.
# Usage:  ./scripts/backup-db.sh [output-dir]
# Output: <output-dir>/citybus-backup-<timestamp>.sql.gz
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OUTPUT_DIR="${1:-$REPO_ROOT/backups}"
mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="$OUTPUT_DIR/citybus-backup-${TIMESTAMP}.sql.gz"

CONTAINER=$(docker compose -f "$REPO_ROOT/docker-compose.yml" ps -q postgres 2>/dev/null || true)
if [ -z "$CONTAINER" ]; then
  echo "ERROR: postgres container not running. Start with 'docker compose up -d postgres' first."
  exit 1
fi

echo "Backing up citybus database..."
docker exec "$CONTAINER" pg_dump -U citybus -d citybus --clean --if-exists | gzip > "$BACKUP_FILE"

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup complete: $BACKUP_FILE ($SIZE)"
