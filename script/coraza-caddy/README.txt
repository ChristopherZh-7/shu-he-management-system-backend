Caddy + OWASP Coraza（替代雷池个人版思路）
========================================

1. 许可证
   - Coraza：Apache-2.0（见 https://github.com/corazawaf/coraza ）
   - Caddy：Apache-2.0
   - 企业使用请以法务对许可证为准。

2. 部署在哪
   - 建议：公网入口「阿里云 ECS」上跑 Caddy+Coraza，与当前架构一致（云反代到 10.40.88.38:8080）。
   - 需本机已能：curl -I http://10.40.88.38:8080/（WireGuard 正常）。

3. 安装（在云 ECS 上）
   sudo bash install-caddy-coraza.sh

   脚本会：安装 go、xcaddy、编译带 coraza-caddy 的 caddy、复制同目录 Caddyfile 到 /etc/caddy/、
   stop/disable nginx、systemctl 启用 caddy。

4. 防火墙 / 安全组
   - 与原先一致：TCP 80、8080 放行；UDP 51820（WireGuard）等。

5. 误报与调参
   - OWASP CRS 可能拦截正常业务，需在 directives 里调规则或阶段性使用 SecRuleEngine DetectionOnly。
   - 详见：https://coreruleset.org/docs/

6. 与 Let’s Encrypt
   - 若 80 站点要自动 HTTPS，在 Caddyfile 里为域名配置 tls 或使用 Caddy 自动 HTTPS（需域名解析到本机）。
   - 保留 /var/www/certbot 仅在你仍用 certbot webroot 时需要；纯 Caddy 可用内置 ACME。

7. 回滚到 Nginx
   sudo systemctl stop caddy && sudo systemctl disable caddy
   sudo systemctl enable nginx && sudo systemctl start nginx
