# Backup the PostgreSQL database from the running Compose stack.
# Usage:  .\scripts\backup-db.ps1 [-OutputDir <path>]
# Output: <OutputDir>\citybus-backup-<timestamp>.sql

param(
    [string]$OutputDir = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")).Path "backups")
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }

$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupFile = Join-Path $OutputDir "citybus-backup-${Timestamp}.sql"

$Container = docker compose -f "$RepoRoot\docker-compose.yml" ps -q postgres 2>$null
if (-not $Container) {
    Write-Error "postgres container not running. Start with 'docker compose up -d postgres' first."
    exit 1
}

Write-Host "Backing up citybus database..."
docker exec $Container pg_dump -U citybus -d citybus --clean --if-exists > $BackupFile

$Size = (Get-Item $BackupFile).Length / 1KB
Write-Host "Backup complete: $BackupFile ($([math]::Round($Size, 1)) KB)"
