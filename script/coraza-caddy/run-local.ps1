# 在本机启动带 Coraza 的 Caddy（仅 127.0.0.1:9180）
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$dist = Join-Path $here "dist"
$caddy = Join-Path $dist "caddy.exe"
$cfg = Join-Path $here "Caddyfile.local-windows"

if (-not (Test-Path $caddy)) {
    Write-Host "未找到 $caddy ，请先在同一目录执行 xcaddy build（或运行过 install 脚本）。"
    exit 1
}

$cfgFull = (Resolve-Path $cfg).Path
Write-Host "启动: $caddy run --config $cfgFull"
Write-Host "浏览器或 curl: http://127.0.0.1:9180/"
Write-Host "Ctrl+C 停止"
Set-Location $dist
& $caddy run --config $cfgFull --adapter caddyfile
