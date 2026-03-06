<#
.SYNOPSIS
    Shuhe Management System - One-click Deployment
.DESCRIPTION
    Deploys database migrations, backend JAR, and/or frontend to the production server.
    Tracks applied SQL migrations to only run new ones automatically.
.EXAMPLE
    .\deploy.ps1                        # Deploy everything (DB + Backend + Frontend)
    .\deploy.ps1 -Backend               # Backend only
    .\deploy.ps1 -Frontend              # Frontend only
    .\deploy.ps1 -Database              # DB migrations only
    .\deploy.ps1 -Backend -Database     # Backend + DB
    .\deploy.ps1 -All -SkipBuild        # Deploy without rebuilding
    .\deploy.ps1 -InitMigrations        # Register all existing migrations as applied (first-time setup)
#>
param(
    [switch]$All,
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$Database,
    [switch]$SkipBuild,
    [switch]$InitMigrations
)

$ErrorActionPreference = "Stop"
$timer = Get-Date

# ============================================================
# Configuration - Modify to match your environment
# ============================================================
$CFG = @{
    Server         = "shkj@10.40.88.38"
    DbHost         = "10.40.88.37"
    DbUser         = "shuhe_prod"
    DbPass         = "Shuhe@Prod2026!"
    DbName         = "shuhe-ms"
    JavaHome       = "C:\Users\Christopher\.jdks\ms-17.0.17"
    MvnCmd         = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd"
    BackendDir     = $PSScriptRoot
    FrontendDir    = "$PSScriptRoot\..\shu-he-management-system-frontend"
    MigrationDir   = "$PSScriptRoot\sql\mysql\migration"
    RemoteBackend  = "/opt/shuhe/backend"
    RemoteFrontend = "/opt/shuhe/frontend"
    RemoteLogs     = "/opt/shuhe/logs"
    JvmOpts        = "-Xms512m -Xmx512m"
    Profile        = "prod"
    HealthTimeout  = 120
}

# Resolve defaults
if ($InitMigrations) { $Database = $true }
if (-not ($All -or $Backend -or $Frontend -or $Database)) { $All = $true }
if ($All) { $Database = $true; $Backend = $true; $Frontend = $true }

# ============================================================
# Utility Functions
# ============================================================

function Write-Step($msg)  { Write-Host "`n[$((Get-Date).ToString('HH:mm:ss'))] $msg" -ForegroundColor Cyan; Write-Host ("-" * 50) -ForegroundColor DarkGray }
function Write-OK($msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Fail($msg)  { Write-Host "  [FAIL] $msg" -ForegroundColor Red }

function Invoke-Remote([string]$Cmd) {
    $out = ssh $CFG.Server $Cmd 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "SSH failed (exit $LASTEXITCODE): $Cmd`n$out" }
    return $out.Trim()
}

function Send-File([string]$Local, [string]$Remote) {
    scp $Local "$($CFG.Server):$Remote" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "SCP failed: $Local" }
}

function Invoke-RemoteSQL([string]$SQL) {
    $tmp = New-TemporaryFile
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($SQL.Replace("`r`n", "`n"))
    [System.IO.File]::WriteAllBytes($tmp.FullName, $bytes)
    Send-File $tmp.FullName "/tmp/_sq.tmp"
    $result = Invoke-Remote "mysql -N -B -h $($CFG.DbHost) -u $($CFG.DbUser) -p'$($CFG.DbPass)' '$($CFG.DbName)' < /tmp/_sq.tmp 2>/dev/null; rm -f /tmp/_sq.tmp"
    Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
    return $result
}

function Send-ShellScript([string]$Content, [string]$RemotePath) {
    $tmp = New-TemporaryFile
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Content.Replace("`r`n", "`n"))
    [System.IO.File]::WriteAllBytes($tmp.FullName, $bytes)
    Send-File $tmp.FullName $RemotePath
    Invoke-Remote "chmod +x $RemotePath" | Out-Null
    Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
}

# ============================================================
# Database Migration
# ============================================================

function Step-Database {
    Write-Step "DATABASE MIGRATION"

    Invoke-RemoteSQL @"
CREATE TABLE IF NOT EXISTS _schema_migrations (
  version VARCHAR(255) NOT NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@
    Write-OK "Migration tracking table ready"

    $appliedRaw = Invoke-RemoteSQL "SELECT version FROM _schema_migrations ORDER BY version;"
    $applied = @()
    if ($appliedRaw) {
        $applied = $appliedRaw -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    }
    Write-Host "  Already applied: $($applied.Count) migration(s)"

    if (-not (Test-Path $CFG.MigrationDir)) {
        Write-Warn "Migration directory not found: $($CFG.MigrationDir)"
        return
    }

    $allFiles = Get-ChildItem $CFG.MigrationDir -Filter "V*.sql" | Sort-Object Name
    $pending = $allFiles | Where-Object { $_.Name -notin $applied }

    if ($pending.Count -eq 0) {
        Write-OK "All migrations up to date"
        return
    }

    # InitMigrations mode: register without executing
    if ($InitMigrations) {
        Write-Warn "InitMigrations mode: registering $($pending.Count) migration(s) as applied..."
        $values = ($pending | ForEach-Object { "('$($_.Name)')" }) -join ","
        Invoke-RemoteSQL "INSERT IGNORE INTO _schema_migrations (version) VALUES $values;"
        Write-OK "Registered $($pending.Count) migration(s)"
        return
    }

    Write-Warn "$($pending.Count) pending migration(s):"
    foreach ($f in $pending) { Write-Host "    -> $($f.Name)" -ForegroundColor Yellow }
    Write-Host ""

    foreach ($file in $pending) {
        Write-Host "  Running: $($file.Name)..." -NoNewline

        Send-File $file.FullName "/tmp/_mig.sql"
        Invoke-Remote "sed -i 's/\r$//' /tmp/_mig.sql"
        $result = Invoke-Remote "mysql -h $($CFG.DbHost) -u $($CFG.DbUser) -p'$($CFG.DbPass)' '$($CFG.DbName)' < /tmp/_mig.sql 2>&1"
        Invoke-Remote "rm -f /tmp/_mig.sql"

        Invoke-RemoteSQL "INSERT INTO _schema_migrations (version) VALUES ('$($file.Name)');"
        Write-Host " OK" -ForegroundColor Green

        if ($result) {
            $filtered = ($result -split "`n" | Where-Object { $_ -notmatch "Warning.*password" -and $_.Trim() }) -join "`n"
            if ($filtered.Trim()) { Write-Host $filtered -ForegroundColor DarkGray }
        }
    }

    Write-OK "All $($pending.Count) migration(s) applied"
}

# ============================================================
# Backend Build & Deploy
# ============================================================

function Step-BuildBackend {
    Write-Step "BUILD BACKEND"
    $env:JAVA_HOME = $CFG.JavaHome
    Write-Host "  JAVA_HOME: $($CFG.JavaHome)"

    Push-Location $CFG.BackendDir
    try {
        $mvn = $CFG.MvnCmd
        cmd /c """$mvn"" clean package -DskipTests -pl shuhe-server -am"
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)" }
        Write-OK "Backend build complete"
    }
    finally { Pop-Location }
}

function Step-DeployBackend {
    Write-Step "DEPLOY BACKEND"

    $jar = Join-Path $CFG.BackendDir "shuhe-server\target\shuhe-server.jar"
    if (-not (Test-Path $jar)) { throw "JAR not found: $jar (run without -SkipBuild)" }

    $sizeMB = "{0:N1}" -f ((Get-Item $jar).Length / 1MB)
    Write-Host "  Uploading shuhe-server.jar ($sizeMB MB)..."
    Send-File $jar "$($CFG.RemoteBackend)/shuhe-server.jar.new"
    Write-OK "JAR uploaded"

    $restartSh = @"
#!/bin/bash
BASE=$($CFG.RemoteBackend)
LOG=$($CFG.RemoteLogs)/shuhe-server.log
PROFILE=$($CFG.Profile)
JVM="$($CFG.JvmOpts)"
echo '[deploy] Stopping...'
PID=`$(ps -ef | grep 'shuhe-server.jar' | grep -v grep | awk '{print `$2}')
if [ -n "`$PID" ]; then
  kill -15 `$PID
  for i in `$(seq 1 15); do sleep 1; kill -0 `$PID 2>/dev/null || break; done
  kill -0 `$PID 2>/dev/null && kill -9 `$PID && sleep 1
  echo "[deploy] Stopped (was PID `$PID)"
else
  echo '[deploy] Not running'
fi
cd `$BASE
cp shuhe-server.jar shuhe-server.jar.bak 2>/dev/null || true
mv shuhe-server.jar.new shuhe-server.jar
echo '[deploy] Starting...'
nohup java `$JVM -jar shuhe-server.jar --spring.profiles.active=`$PROFILE > `$LOG 2>&1 &
disown
echo "[deploy] Started PID `$!"
"@
    Send-ShellScript $restartSh "/tmp/_restart.sh"
    Write-Host "  Restarting service..."
    $output = Invoke-Remote "bash /tmp/_restart.sh; rm -f /tmp/_restart.sh"
    Write-Host $output

    Write-Host "  Health check" -NoNewline
    $healthy = $false
    for ($i = 0; $i -lt $CFG.HealthTimeout; $i += 2) {
        Start-Sleep -Seconds 2
        $code = Invoke-Remote "curl -s -o /dev/null -w '%{http_code}' http://localhost:48080/actuator/health 2>/dev/null || echo 000"
        if ($code.Trim() -eq "200") { $healthy = $true; break }
        Write-Host "." -NoNewline
    }
    Write-Host ""

    if ($healthy) { Write-OK "Backend is healthy" }
    else { Write-Fail "Health check timeout! Run: ssh $($CFG.Server) 'tail -50 $($CFG.RemoteLogs)/shuhe-server.log'" }
}

# ============================================================
# Frontend Build & Deploy
# ============================================================

function Step-BuildFrontend {
    Write-Step "BUILD FRONTEND"
    $dir = Resolve-Path $CFG.FrontendDir -ErrorAction Stop
    Write-Host "  Directory: $dir"

    Push-Location $dir
    try {
        pnpm run build:antd
        if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
        Write-OK "Frontend build complete"
    }
    finally { Pop-Location }
}

function Step-DeployFrontend {
    Write-Step "DEPLOY FRONTEND"

    $zip = Join-Path (Resolve-Path $CFG.FrontendDir) "apps\web-antd\dist.zip"
    if (-not (Test-Path $zip)) { throw "dist.zip not found: $zip (run without -SkipBuild)" }

    $sizeMB = "{0:N1}" -f ((Get-Item $zip).Length / 1MB)
    Write-Host "  Uploading dist.zip ($sizeMB MB)..."
    Send-File $zip "/tmp/_frontend.zip"
    Write-OK "Uploaded"

    $ts = (Get-Date).ToString("yyyyMMddHHmm")
    Write-Host "  Deploying files (backup as frontend.bak.$ts)..."
    Invoke-Remote "cd /opt/shuhe && mv frontend frontend.bak.$ts && mkdir frontend && cd frontend && unzip -q /tmp/_frontend.zip && rm -f /tmp/_frontend.zip"

    # Keep only 3 most recent backups
    Invoke-Remote "cd /opt/shuhe && ls -dt frontend.bak.* 2>/dev/null | tail -n +4 | xargs rm -rf 2>/dev/null || true"
    Write-OK "Frontend deployed"
}

# ============================================================
# Main
# ============================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  Shuhe Management System - Deploy" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

$targets = @()
if ($Database) { $targets += "Database" }
if ($Backend)  { $targets += "Backend" }
if ($Frontend) { $targets += "Frontend" }

Write-Host "  Targets : $($targets -join ' + ')"
Write-Host "  Server  : $($CFG.Server)"
Write-Host "  Database: $($CFG.DbHost)/$($CFG.DbName)"
if ($SkipBuild) { Write-Host "  Build   : SKIPPED" -ForegroundColor Yellow }
if ($InitMigrations) { Write-Host "  Mode    : INIT MIGRATIONS (register only)" -ForegroundColor Yellow }
Write-Host ""

Write-Step "VERIFY CONNECTION"
$test = Invoke-Remote "echo ok"
if ($test -ne "ok") { throw "Cannot connect to $($CFG.Server)" }
Write-OK "SSH connected"

try {
    if ($Database) { Step-Database }
    if ($Backend -and -not $SkipBuild)  { Step-BuildBackend }
    if ($Backend)  { Step-DeployBackend }
    if ($Frontend -and -not $SkipBuild) { Step-BuildFrontend }
    if ($Frontend) { Step-DeployFrontend }

    $elapsed = (Get-Date) - $timer
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  DEPLOYMENT COMPLETE" -ForegroundColor Green
    Write-Host "  Time: $("{0:mm\:ss}" -f $elapsed)" -ForegroundColor Green
    Write-Host "  URL:  http://10.40.88.38" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}
catch {
    Write-Fail "DEPLOYMENT FAILED: $_"
    exit 1
}
