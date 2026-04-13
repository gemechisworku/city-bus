# Restore a PostgreSQL backup into the running Compose stack.
# Usage:  .\scripts\restore-db.ps1 -BackupFile <path-to-backup.sql>
# WARNING: This replaces the current database contents.

param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if (-not (Test-Path $BackupFile)) {
    Write-Error "File not found: $BackupFile"
    exit 1
}

$Container = docker compose -f "$RepoRoot\docker-compose.yml" ps -q postgres 2>$null
if (-not $Container) {
    Write-Error "postgres container not running. Start with 'docker compose up -d postgres' first."
    exit 1
}

Write-Host "WARNING: This will replace the current database contents."
Write-Host "Restoring from: $BackupFile"

Get-Content $BackupFile -Raw | docker exec -i $Container psql -U citybus -d citybus --single-transaction

Write-Host "Restore complete."
