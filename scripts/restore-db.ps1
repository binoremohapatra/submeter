# ── SubMeter DB Restore Script ────────────────────────────────────────────────
# Usage:
#   .\scripts\restore-db.ps1                       <- restores latest backup
#   .\scripts\restore-db.ps1 -File "submeter_20260715_123456.sql"
# ─────────────────────────────────────────────────────────────────────────────

param(
    [string]$File = ""
)

$ErrorActionPreference = "Stop"
$BackupDir = Join-Path $PSScriptRoot "backups"

# Check postgres container is running
$running = docker inspect -f "{{.State.Running}}" submeter-postgres 2>$null
if ($running -ne "true") {
    Write-Error "submeter-postgres container is not running. Start it with: docker compose up -d postgres"
    exit 1
}

# Resolve which file to restore
if ($File -eq "") {
    # Pick the newest backup automatically
    $latest = Get-ChildItem -Path $BackupDir -Filter "submeter_*.sql" |
              Sort-Object LastWriteTime -Descending |
              Select-Object -First 1

    if ($null -eq $latest) {
        Write-Error "No backup files found in $BackupDir. Run backup-db.ps1 first."
        exit 1
    }
    $RestoreFile = $latest.FullName
    Write-Host "Auto-selected latest backup: $($latest.Name)" -ForegroundColor Yellow
} else {
    $RestoreFile = if ([System.IO.Path]::IsPathRooted($File)) { $File } else { Join-Path $BackupDir $File }
    if (-not (Test-Path $RestoreFile)) {
        Write-Error "Backup file not found: $RestoreFile"
        exit 1
    }
}

Write-Host ""
Write-Host "WARNING: This will DROP and recreate all tables in submeter DB!" -ForegroundColor Red
$confirm = Read-Host "Type YES to continue"
if ($confirm -ne "YES") {
    Write-Host "Aborted." -ForegroundColor DarkGray
    exit 0
}

Write-Host "Restoring from $RestoreFile ..." -ForegroundColor Cyan

# Drop and recreate the public schema to wipe all tables cleanly
docker exec submeter-postgres psql -U submeter -d submeter -c `
    "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" | Out-Null

# Restore
Get-Content $RestoreFile -Raw |
    docker exec -i submeter-postgres psql -U submeter -d submeter

Write-Host "Restore complete!" -ForegroundColor Green
