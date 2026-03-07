<#
.SYNOPSIS
    Shuhe Management System - Production Database Rebuild
.DESCRIPTION
    Cleans test data from local database, creates a clean backup, and
    rebuilds the production database from scratch.

    This script is for major schema changes where incremental migrations
    are insufficient or risky.
.EXAMPLE
    .\rebuild_production.ps1                    # Full rebuild: clean + backup + deploy to production
    .\rebuild_production.ps1 -CleanOnly         # Only clean local DB, don't touch production
    .\rebuild_production.ps1 -SkipClean         # Use already-clean local DB, just rebuild production
    .\rebuild_production.ps1 -DeployAll         # After DB rebuild, also deploy backend + frontend
    .\rebuild_production.ps1 -DryRun            # Show what would happen, don't execute
#>
param(
    [switch]$CleanOnly,
    [switch]$SkipClean,
    [switch]$DeployAll,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$timer = Get-Date

# ============================================================
# Configuration
# ============================================================
$CFG = @{
    # Local database
    LocalDbHost    = "127.0.0.1"
    LocalDbPort    = "3306"
    LocalDbUser    = "root"
    LocalDbPass    = "123456"
    LocalDbName    = "shuhe-ms"

    # Production server (SSH jump host)
    Server         = "shkj@10.40.88.38"

    # Production database (accessed from SSH host)
    ProdDbHost     = "10.40.88.37"
    ProdDbUser     = "shuhe_prod"
    ProdDbPass     = "Shuhe@Prod2026!"
    ProdDbName     = "shuhe-ms"

    # Paths
    BackupDir      = "$PSScriptRoot\sql\backup"
    MigrationDir   = "$PSScriptRoot\sql\mysql\migration"
    CleanSqlFile   = "$PSScriptRoot\sql\clean_test_data.sql"
}

# ============================================================
# Utility Functions
# ============================================================

function Write-Step($msg)  { Write-Host "`n[$((Get-Date).ToString('HH:mm:ss'))] $msg" -ForegroundColor Cyan; Write-Host ("-" * 60) -ForegroundColor DarkGray }
function Write-OK($msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Fail($msg)  { Write-Host "  [FAIL] $msg" -ForegroundColor Red }

function Invoke-LocalSQL([string]$SQL) {
    $result = $SQL | mysql -h $CFG.LocalDbHost -P $CFG.LocalDbPort -u $CFG.LocalDbUser -p"$($CFG.LocalDbPass)" $CFG.LocalDbName 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Local MySQL failed: $result" }
    return $result
}

function Invoke-LocalSQLFile([string]$FilePath) {
    $result = mysql -h $CFG.LocalDbHost -P $CFG.LocalDbPort -u $CFG.LocalDbUser -p"$($CFG.LocalDbPass)" $CFG.LocalDbName -e "source $FilePath" 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Local MySQL file execution failed: $result" }
    return $result
}

function Invoke-Remote([string]$Cmd) {
    $out = ssh $CFG.Server $Cmd 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "SSH failed (exit $LASTEXITCODE): $Cmd`n$out" }
    return $out.Trim()
}

function Send-File([string]$Local, [string]$Remote) {
    scp $Local "$($CFG.Server):$Remote" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "SCP failed: $Local -> $Remote" }
}

function Invoke-RemoteSQL([string]$SQL) {
    $tmp = New-TemporaryFile
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($SQL.Replace("`r`n", "`n"))
    [System.IO.File]::WriteAllBytes($tmp.FullName, $bytes)
    Send-File $tmp.FullName "/tmp/_rebuild_sq.tmp"
    $result = Invoke-Remote "mysql -N -B -h $($CFG.ProdDbHost) -u $($CFG.ProdDbUser) -p'$($CFG.ProdDbPass)' '$($CFG.ProdDbName)' < /tmp/_rebuild_sq.tmp 2>/dev/null; rm -f /tmp/_rebuild_sq.tmp"
    Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
    return $result
}

# ============================================================
# MAIN FLOW
# ============================================================

Write-Host ""
Write-Host "=============================================" -ForegroundColor Magenta
Write-Host "  Shuhe - Production Database Rebuild" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta

$mode = @()
if (-not $SkipClean) { $mode += "Clean local DB" }
$mode += "Create backup"
if (-not $CleanOnly)  { $mode += "Rebuild production DB" }
if ($DeployAll)       { $mode += "Deploy backend+frontend" }

Write-Host "  Steps   : $($mode -join ' -> ')"
Write-Host "  Local DB: $($CFG.LocalDbHost):$($CFG.LocalDbPort)/$($CFG.LocalDbName)"
if (-not $CleanOnly) {
    Write-Host "  Prod DB : $($CFG.ProdDbHost)/$($CFG.ProdDbName)"
    Write-Host "  Server  : $($CFG.Server)"
}
if ($DryRun) { Write-Host "  Mode    : DRY RUN (no changes)" -ForegroundColor Yellow }
Write-Host ""

# ============================================================
# Step 1: Verify local MySQL connection
# ============================================================
Write-Step "VERIFY LOCAL DATABASE"

try {
    $ver = Invoke-LocalSQL "SELECT VERSION();"
    Write-OK "Local MySQL connected (version: $($ver.Trim()))"
}
catch {
    Write-Fail "Cannot connect to local MySQL at $($CFG.LocalDbHost):$($CFG.LocalDbPort)"
    Write-Host "  Ensure MySQL is running and credentials are correct." -ForegroundColor Yellow
    exit 1
}

$tableCount = Invoke-LocalSQL "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$($CFG.LocalDbName)';"
Write-Host "  Tables in '$($CFG.LocalDbName)': $($tableCount.Trim())"

# ============================================================
# Step 2: Clean local database
# ============================================================
if (-not $SkipClean) {
    Write-Step "CLEAN LOCAL DATABASE TEST DATA"

    if (-not (Test-Path $CFG.CleanSqlFile)) {
        throw "Clean SQL file not found: $($CFG.CleanSqlFile)"
    }

    Write-Warn "This will DELETE all business test data from your local database!"
    Write-Host "  Affected: CRM, Projects, Allocations, Flowable history, Logs, Demos" -ForegroundColor Yellow
    Write-Host "  Preserved: System config, Users, Roles, Menus, Depts, BPM definitions" -ForegroundColor Yellow
    Write-Host ""

    if (-not $DryRun) {
        $confirm = Read-Host "  Continue? (yes/no)"
        if ($confirm -ne "yes") {
            Write-Warn "Aborted by user"
            exit 0
        }

        Write-Host "  Running clean_test_data.sql..."
        $cleanOutput = Invoke-LocalSQLFile $CFG.CleanSqlFile
        if ($cleanOutput) {
            $cleanOutput -split "`n" | ForEach-Object {
                if ($_.Trim()) { Write-Host "    $_" -ForegroundColor DarkGray }
            }
        }
        Write-OK "Local database cleaned"
    }
    else {
        Write-Host "  [DRY RUN] Would run: $($CFG.CleanSqlFile)" -ForegroundColor DarkGray
    }
}
else {
    Write-Step "SKIP CLEAN (using current local database state)"
}

# ============================================================
# Step 3: Create clean backup
# ============================================================
Write-Step "CREATE CLEAN BACKUP"

if (-not (Test-Path $CFG.BackupDir)) {
    New-Item -ItemType Directory -Path $CFG.BackupDir -Force | Out-Null
}

$timestamp = (Get-Date).ToString("yyyyMMdd_HHmmss")
$backupFile = Join-Path $CFG.BackupDir "shuhe-ms_clean_$timestamp.sql"

if (-not $DryRun) {
    Write-Host "  Dumping database..."
    mysqldump -h $CFG.LocalDbHost -P $CFG.LocalDbPort -u $CFG.LocalDbUser -p"$($CFG.LocalDbPass)" `
        --default-character-set=utf8mb4 `
        --set-charset `
        --single-transaction `
        --routines `
        --triggers `
        --set-gtid-purged=OFF `
        $CFG.LocalDbName -r $backupFile

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backupFile)) {
        throw "mysqldump failed!"
    }

    $sizeMB = "{0:N1}" -f ((Get-Item $backupFile).Length / 1MB)
    Write-OK "Backup created: $backupFile ($sizeMB MB)"
}
else {
    Write-Host "  [DRY RUN] Would create: $backupFile" -ForegroundColor DarkGray
}

# If CleanOnly, stop here
if ($CleanOnly) {
    $elapsed = (Get-Date) - $timer
    Write-Host ""
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host "  LOCAL CLEANUP COMPLETE" -ForegroundColor Green
    Write-Host "  Backup: $backupFile" -ForegroundColor Green
    Write-Host "  Time: $("{0:mm\:ss}" -f $elapsed)" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Next step: run without -CleanOnly to deploy to production" -ForegroundColor Yellow
    exit 0
}

# ============================================================
# Step 4: Verify SSH connection
# ============================================================
Write-Step "VERIFY PRODUCTION CONNECTION"

$test = Invoke-Remote "echo ok"
if ($test -ne "ok") { throw "Cannot connect to $($CFG.Server)" }
Write-OK "SSH connected to $($CFG.Server)"

# ============================================================
# Step 5: Backup production database (safety net)
# ============================================================
Write-Step "BACKUP PRODUCTION DATABASE (safety net)"

if (-not $DryRun) {
    $prodBackup = "/tmp/shuhe_prod_backup_$timestamp.sql"
    Write-Host "  Backing up production DB to $prodBackup on remote..."
    Invoke-Remote "mysqldump -h $($CFG.ProdDbHost) -u $($CFG.ProdDbUser) -p'$($CFG.ProdDbPass)' --default-character-set=utf8mb4 --single-transaction --routines --triggers --set-gtid-purged=OFF '$($CFG.ProdDbName)' > $prodBackup 2>/dev/null"
    $prodSize = Invoke-Remote "du -h $prodBackup | cut -f1"
    Write-OK "Production backup: $prodBackup ($prodSize)"
}
else {
    Write-Host "  [DRY RUN] Would backup production DB" -ForegroundColor DarkGray
}

# ============================================================
# Step 6: Upload clean backup to server
# ============================================================
Write-Step "UPLOAD CLEAN BACKUP TO SERVER"

$remoteCleanFile = "/tmp/shuhe_clean_import.sql"

if (-not $DryRun) {
    $sizeMB = "{0:N1}" -f ((Get-Item $backupFile).Length / 1MB)
    Write-Host "  Uploading $sizeMB MB..."
    Send-File $backupFile $remoteCleanFile
    Write-OK "Upload complete"
}
else {
    Write-Host "  [DRY RUN] Would upload $backupFile to $remoteCleanFile" -ForegroundColor DarkGray
}

# ============================================================
# Step 7: Drop and recreate production database
# ============================================================
Write-Step "REBUILD PRODUCTION DATABASE"

Write-Warn "This will DROP and RECREATE the production database!"
Write-Host "  Database: $($CFG.ProdDbHost)/$($CFG.ProdDbName)" -ForegroundColor Red
Write-Host "  A safety backup was created in Step 5." -ForegroundColor Yellow
Write-Host ""

if (-not $DryRun) {
    $confirm2 = Read-Host "  Type 'REBUILD' to confirm (case-sensitive)"
    if ($confirm2 -ne "REBUILD") {
        Write-Warn "Aborted by user (cleanup file preserved on server: $remoteCleanFile)"
        exit 0
    }

    Write-Host "  Dropping database..."
    Invoke-Remote "mysql -h $($CFG.ProdDbHost) -u $($CFG.ProdDbUser) -p'$($CFG.ProdDbPass)' -e ""DROP DATABASE IF EXISTS ``shuhe-ms``; CREATE DATABASE ``shuhe-ms`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"" 2>/dev/null"
    Write-OK "Database dropped and recreated"

    Write-Host "  Importing clean backup (this may take a while)..."
    Invoke-Remote "mysql -h $($CFG.ProdDbHost) -u $($CFG.ProdDbUser) -p'$($CFG.ProdDbPass)' '$($CFG.ProdDbName)' < $remoteCleanFile 2>/dev/null"
    Write-OK "Import complete"

    Invoke-Remote "rm -f $remoteCleanFile"
}
else {
    Write-Host "  [DRY RUN] Would drop and recreate '$($CFG.ProdDbName)'" -ForegroundColor DarkGray
    Write-Host "  [DRY RUN] Would import clean backup" -ForegroundColor DarkGray
}

# ============================================================
# Step 8: Register all migrations as applied
# ============================================================
Write-Step "REGISTER ALL MIGRATIONS"

$allMigrations = Get-ChildItem $CFG.MigrationDir -Filter "V*.sql" | Sort-Object Name
Write-Host "  Total migration files: $($allMigrations.Count)"

if (-not $DryRun) {
    $createTable = @"
CREATE TABLE IF NOT EXISTS _schema_migrations (
  version VARCHAR(255) NOT NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@
    Invoke-RemoteSQL $createTable

    $batchSize = 50
    for ($i = 0; $i -lt $allMigrations.Count; $i += $batchSize) {
        $batch = $allMigrations[$i..([Math]::Min($i + $batchSize - 1, $allMigrations.Count - 1))]
        $values = ($batch | ForEach-Object { "('$($_.Name)')" }) -join ","
        Invoke-RemoteSQL "INSERT IGNORE INTO _schema_migrations (version) VALUES $values;"
    }

    $registered = Invoke-RemoteSQL "SELECT COUNT(*) FROM _schema_migrations;"
    Write-OK "Registered $($registered.Trim()) migrations"
}
else {
    Write-Host "  [DRY RUN] Would register $($allMigrations.Count) migrations" -ForegroundColor DarkGray
}

# ============================================================
# Step 9: Verify production database
# ============================================================
Write-Step "VERIFY PRODUCTION DATABASE"

if (-not $DryRun) {
    $prodTables = Invoke-RemoteSQL "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$($CFG.ProdDbName)';"
    Write-OK "Production tables: $($prodTables.Trim())"

    $bizCheck = Invoke-RemoteSQL "SELECT COUNT(*) FROM crm_business;"
    $projCheck = Invoke-RemoteSQL "SELECT COUNT(*) FROM project;"
    Write-OK "Business data: $($bizCheck.Trim()) records (should be 0)"
    Write-OK "Project data: $($projCheck.Trim()) records (should be 0)"

    $userCheck = Invoke-RemoteSQL "SELECT COUNT(*) FROM system_users WHERE deleted = 0;"
    $menuCheck = Invoke-RemoteSQL "SELECT COUNT(*) FROM system_menu WHERE deleted = 0;"
    Write-OK "System users: $($userCheck.Trim())"
    Write-OK "System menus: $($menuCheck.Trim())"
}

# ============================================================
# Step 10: Optionally deploy backend + frontend
# ============================================================
if ($DeployAll) {
    Write-Step "DEPLOYING BACKEND + FRONTEND"
    Write-Host "  Calling deploy.ps1 -Backend -Frontend ..."
    & "$PSScriptRoot\deploy.ps1" -Backend -Frontend
}

# ============================================================
# Done
# ============================================================
$elapsed = (Get-Date) - $timer

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  PRODUCTION REBUILD COMPLETE" -ForegroundColor Green
Write-Host "  Time: $("{0:mm\:ss}" -f $elapsed)" -ForegroundColor Green
if (-not $DryRun) {
    Write-Host "  Safety backup: /tmp/shuhe_prod_backup_$timestamp.sql" -ForegroundColor Green
    Write-Host "  Clean backup : $backupFile" -ForegroundColor Green
}
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""

if (-not $DeployAll -and -not $CleanOnly) {
    Write-Host "  Next: deploy backend + frontend" -ForegroundColor Yellow
    Write-Host "    .\deploy.ps1 -Backend -Frontend" -ForegroundColor Yellow
    Write-Host "  Or run this script with -DeployAll to do it all at once" -ForegroundColor Yellow
}
