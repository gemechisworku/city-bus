#!/usr/bin/env bash
# Restore a PostgreSQL backup into the running Compose stack.
# Usage:  ./scripts/restore-db.sh <backup-file.sql.gz>
# WARNING: This replaces the current database contents.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -z "${1:-}" ]; then
  echo "Usage: $0 <backup-file.sql.gz>"
  echo "  Restores a gzipped SQL dump into the running postgres container."
  exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: File not found: $BACKUP_FILE"
  exit 1
fi

CONTAINER=$(docker compose -f "$REPO_ROOT/docker-compose.yml" ps -q postgres 2>/dev/null || true)
if [ -z "$CONTAINER" ]; then
  echo "ERROR: postgres container not running. Start with 'docker compose up -d postgres' first."
  exit 1
fi

echo "WARNING: This will replace the current database contents."
echo "Restoring from: $BACKUP_FILE"

gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" psql -U citybus -d citybus --single-transaction

echo "Restore complete."
