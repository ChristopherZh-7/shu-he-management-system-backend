<#
.SYNOPSIS
    从 10.40.88.37 生产库导出数据库，并导入到本地 MySQL 用于测试
.DESCRIPTION
    1. 通过 SSH 跳板机 (10.40.88.38) 在远端执行 mysqldump
    2. 将导出的 .sql 文件 SCP 回本地 sql/ 目录
    3. 导入到本地 MySQL (127.0.0.1:3306/shuhe-ms)
.EXAMPLE
    .\dump_prod_to_local.ps1              # 完整流程：导出 + 导入
    .\dump_prod_to_local.ps1 -DumpOnly    # 仅导出，不导入
    .\dump_prod_to_local.ps1 -ImportOnly  # 仅导入（使用已有 dump 文件）
#>
param(
    [switch]$DumpOnly,
    [switch]$ImportOnly
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Split-Path -Parent $scriptDir

# ============================================================
# 配置（与 deploy.ps1 / application-prod.yaml 一致）
# ============================================================
$CFG = @{
    # SSH 跳板机（可访问生产库的机器）
    Server = "shkj@10.40.88.38"

    # 生产数据库 (10.40.88.37)
    ProdDbHost = "10.40.88.37"
    ProdDbUser = "shuhe_prod"
    ProdDbPass = "Shuhe@Prod2026!"
    ProdDbName = "shuhe-ms"

    # 本地数据库 (application-local.yaml)
    LocalDbHost = "127.0.0.1"
    LocalDbPort = "3306"
    LocalDbUser = "root"
    LocalDbPass = "123456"
    LocalDbName = "shuhe-ms"

    # 导出文件保存路径
    DumpDir = $scriptDir
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$dumpFile = Join-Path $CFG.DumpDir "shuhe-ms_prod_$timestamp.sql"
$remoteDumpPath = "/tmp/shuhe-ms_prod_$timestamp.sql"

function Write-Step($msg) { Write-Host "`n[$((Get-Date).ToString('HH:mm:ss'))] $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red }

function Invoke-Remote([string]$Cmd) {
    $out = ssh $CFG.Server $Cmd 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "SSH 失败 (exit $LASTEXITCODE): $Cmd`n$out" }
    return $out.Trim()
}

function Send-File([string]$Local, [string]$Remote) {
    scp $Local "$($CFG.Server):$Remote" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "SCP 失败: $Local -> $Remote" }
}

function Receive-File([string]$Remote, [string]$Local) {
    scp "$($CFG.Server):$Remote" $Local 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "SCP 失败: $Remote -> $Local" }
}

# ============================================================
# 主流程
# ============================================================
Write-Host ""
Write-Host "=============================================" -ForegroundColor Magenta
Write-Host "  生产库 -> 本地 数据库同步" -ForegroundColor Magenta
Write-Host "=============================================" -ForegroundColor Magenta
Write-Host "  生产库: $($CFG.ProdDbHost)/$($CFG.ProdDbName)"
Write-Host "  本地库: $($CFG.LocalDbHost):$($CFG.LocalDbPort)/$($CFG.LocalDbName)"
Write-Host "  导出文件: $dumpFile"
Write-Host ""

# Step 1: 导出生产库
if (-not $ImportOnly) {
    Write-Step "Step 1: 从生产库导出 (mysqldump via SSH)"
    try {
        # 在跳板机上执行 mysqldump，输出到 /tmp
        $dumpCmd = "mysqldump -h $($CFG.ProdDbHost) -u $($CFG.ProdDbUser) -p'$($CFG.ProdDbPass)' --single-transaction --routines --triggers --default-character-set=utf8mb4 $($CFG.ProdDbName) > $remoteDumpPath 2>/dev/null"
        Invoke-Remote $dumpCmd
        Write-OK "导出完成: $remoteDumpPath"
    } catch {
        Write-Fail $_.Exception.Message
        exit 1
    }

    Write-Step "Step 2: 将 dump 文件拉取到本地"
    try {
        Receive-File $remoteDumpPath $dumpFile
        Invoke-Remote "rm -f $remoteDumpPath"  # 清理远端临时文件
        Write-OK "已保存到: $dumpFile"
    } catch {
        Write-Fail $_.Exception.Message
        exit 1
    }

    if ($DumpOnly) {
        Write-Host "`n[DumpOnly] 仅导出，不导入。文件已保存: $dumpFile" -ForegroundColor Yellow
        exit 0
    }
} else {
    # ImportOnly: 使用最新的 dump 文件
    $latest = Get-ChildItem -Path $CFG.DumpDir -Filter "shuhe-ms_prod_*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) {
        Write-Fail "未找到 shuhe-ms_prod_*.sql 文件，请先执行导出"
        exit 1
    }
    $dumpFile = $latest.FullName
    Write-Host "使用已有文件: $dumpFile" -ForegroundColor Gray
}

# Step 3: 导入到本地
Write-Step "Step 3: 导入到本地 MySQL"
try {
    # 先创建数据库（若不存在）
    $createDb = "CREATE DATABASE IF NOT EXISTS ``$($CFG.LocalDbName)`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $createDb | mysql -h $CFG.LocalDbHost -P $CFG.LocalDbPort -u $CFG.LocalDbUser -p"$($CFG.LocalDbPass)" 2>&1 | Out-Null

    # 导入：使用 cmd 重定向，避免 PowerShell pipe 编码问题
    $mysqlArgs = "-h $($CFG.LocalDbHost) -P $($CFG.LocalDbPort) -u $($CFG.LocalDbUser) -p$($CFG.LocalDbPass) $($CFG.LocalDbName)"
    $proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c mysql $mysqlArgs < `"$dumpFile`"" -Wait -PassThru -NoNewWindow
    if ($proc.ExitCode -ne 0) {
        throw "mysql 导入返回 exit code $($proc.ExitCode)"
    }
    Write-OK "导入完成"
} catch {
    Write-Warn "若自动导入失败，可手动在 CMD 中执行:"
    Write-Host "  mysql -h 127.0.0.1 -u root -p123456 shuhe-ms < `"$dumpFile`"" -ForegroundColor Gray
    Write-Fail $_.Exception.Message
    exit 1
}

Write-Host ""
Write-Host "完成。本地数据库已更新，可使用 application-local 配置启动后端测试。" -ForegroundColor Green
Write-Host ""
