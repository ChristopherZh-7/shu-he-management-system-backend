SafeLine (雷池) + 数和管理系统 — 服务器侧说明
============================================

1) 雷池目录: /data/safeline
   启动: cd /data/safeline && docker compose -f compose.yaml --env-file .env up -d

2) 管理控制台: https://<服务器IP>:9443 （本机 .env 中 MGT_PORT=9443）

3) 内层 Nginx（给雷池做上游）: Docker 容器 shuhe-safeline-upstream
   - 使用 Host 网络，监听 8080
   - 配置文件模板在本仓库 script/safeline/nginx-upstream-8080.conf
   - 服务器上路径示例: /home/shkj/shuhe-safeline-upstream.conf

4) 在雷池控制台「添加站点」：
   - 代理到: http://127.0.0.1:8080
   - 不要用 http://127.0.0.1:80（会与已释放的原 Nginx 冲突；未添加站点前 tengine 也不监听 80）

5) 重要：原系统 Nginx (systemd) 已执行 quit，但服务仍为 enabled。
   重启机器后若 system nginx 再次占用 80，会与雷池冲突。请在本机执行一次（需可输入 sudo 密码）:
     sudo systemctl disable nginx --now

6) 仅内网直访业务（不经雷池）: http://<IP>:8080
