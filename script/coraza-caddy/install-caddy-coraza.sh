#!/usr/bin/env bash
# 在 Ubuntu/Debian 云 ECS 上以 root 或 sudo 执行
# 作用：编译带 Coraza 的 Caddy、安装 systemd、停用 nginx 后由 Caddy 监听 80/8080
set -euo pipefail

CADDY_INSTALL="/usr/local/bin/caddy"
CFG_DIR="/etc/caddy"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> 安装依赖（go、curl、git）"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y curl git ca-certificates golang-go build-essential

echo "==> 安装 xcaddy"
export GOTOOLCHAIN=local
go install github.com/caddyserver/xcaddy/cmd/xcaddy@latest
export PATH="${PATH}:$(go env GOPATH)/bin"

echo "==> 编译 Caddy + coraza-caddy（可能需要几分钟）"
cd /tmp
rm -f caddy
xcaddy build --with github.com/corazawaf/coraza-caddy/v2
install -m 0755 caddy "${CADDY_INSTALL}"
"${CADDY_INSTALL}" version

echo "==> 配置目录与 Caddyfile"
mkdir -p "${CFG_DIR}"
if [[ -f "${SCRIPT_DIR}/Caddyfile" ]]; then
  cp -a "${SCRIPT_DIR}/Caddyfile" "${CFG_DIR}/Caddyfile"
else
  echo "未找到同目录 Caddyfile，请手动创建 ${CFG_DIR}/Caddyfile"
  exit 1
fi

mkdir -p /var/www/official /var/www/certbot/.well-known/acme-challenge
[[ -f /var/www/official/index.html ]] || echo '<h1>官网占位</h1>' > /var/www/official/index.html

echo "==> 停用 nginx（避免与 Caddy 抢 80/8080）"
systemctl stop nginx 2>/dev/null || true
systemctl disable nginx 2>/dev/null || true

echo "==> systemd: caddy"
cat > /etc/systemd/system/caddy.service << 'EOF'
[Unit]
Description=Caddy with Coraza WAF
Documentation=https://caddyserver.com/docs/
After=network.target network-online.target
Wants=network-online.target

[Service]
Type=notify
User=root
Group=root
ExecStart=/usr/local/bin/caddy run --environ --config /etc/caddy/Caddyfile
ExecReload=/usr/local/bin/caddy reload --config /etc/caddy/Caddyfile --force
TimeoutStopSec=5s
LimitNOFILE=1048576
PrivateTmp=true
AmbientCapabilities=CAP_NET_BIND_SERVICE

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable caddy
systemctl restart caddy
sleep 2
systemctl --no-pager status caddy || true

echo ""
echo "==> 本机自检"
curl -sI -o /dev/null -w "HTTP %{http_code} :80\n" http://127.0.0.1/ || true
curl -sI -o /dev/null -w "HTTP %{http_code} :8080\n" http://127.0.0.1:8080/ || true

echo ""
echo "完成。若 8080 非 200：确认 WireGuard 通且内网 10.40.88.38:8080 可访问。"
echo "查看日志: journalctl -u caddy -f"
