# ── SubMeter DB Backup Script ─────────────────────────────────────────────────
# Usage:
#   .\scripts\backup-db.ps1           <- manual backup
#   .\scripts\backup-db.ps1 -Keep 5   <- keep only last 5 backups (default: 10)
#
# Backups land in: scripts\backups\submeter_YYYYMMDD_HHMMSS.sql
# ─────────────────────────────────────────────────────────────────────────────

param(
    [int]$Keep = 10
)

$ErrorActionPreference = "Stop"

$BackupDir  = Join-Path $PSScriptRoot "backups"
$Timestamp  = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = Join-Path $BackupDir "submeter_$Timestamp.sql"

# Make sure backup dir exists
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

# Check postgres container is running
$running = docker inspect -f "{{.State.Running}}" submeter-postgres 2>$null
if ($running -ne "true") {
    Write-Error "submeter-postgres container is not running. Start it with: docker compose up -d postgres"
    exit 1
}

Write-Host "Backing up database to $BackupFile ..." -ForegroundColor Cyan

docker exec submeter-postgres pg_dump `
    -U submeter `
    -d submeter `
    --no-owner `
    --no-acl `
    -F plain `
    | Out-File -FilePath $BackupFile -Encoding UTF8

$sizeMB = [math]::Round((Get-Item $BackupFile).Length / 1MB, 2)
Write-Host "Backup complete! Size: ${sizeMB} MB" -ForegroundColor Green

# Prune old backups — keep only latest $Keep files
$allBackups = Get-ChildItem -Path $BackupDir -Filter "submeter_*.sql" |
              Sort-Object LastWriteTime -Descending

if ($allBackups.Count -gt $Keep) {
    $toDelete = $allBackups | Select-Object -Skip $Keep
    foreach ($f in $toDelete) {
        Remove-Item $f.FullName -Force
        Write-Host "Removed old backup: $($f.Name)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "All backups in $BackupDir :" -ForegroundColor DarkCyan
Get-ChildItem -Path $BackupDir -Filter "submeter_*.sql" |
    Sort-Object LastWriteTime -Descending |
    ForEach-Object { Write-Host "  $($_.Name)  ($([math]::Round($_.Length/1KB,1)) KB)" }
