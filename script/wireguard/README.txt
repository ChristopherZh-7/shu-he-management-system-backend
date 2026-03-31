WireGuard：云机访问内网 10.40.88.x，再在云上 80/8080 反代
============================================================

前提
----
- 云安全组 + 本机 ufw：已放行 UDP 51820，TCP 80/8080。
- 内网选一台长期在线 Linux，能访问 10.40.88.38（及目标端口）。

本目录文件
----------
- wg0-cloud.conf.example    → 拷贝为云机 /etc/wireguard/wg0.conf
- wg0-office.conf.example    → 拷贝为内网机 /etc/wireguard/wg0.conf
- nginx-shuhe-proxy.conf    → 拷贝为云机 Nginx site（反代到内网 8080）
- generate-keys.sh          → 在任意 Linux 上生成两对密钥（仅打印，不落盘私钥可选用）

快速步骤
--------
1) 把 generate-keys.sh 传到一台 Linux 执行：bash generate-keys.sh
   记下 cloud / office 的 PrivateKey、PublicKey。

2) 编辑 wg0-cloud.conf.example：
   - <CLOUD_PRIVATE_KEY>
   - <OFFICE_PUBLIC_KEY>

3) 编辑 wg0-office.conf.example：
   - <OFFICE_PRIVATE_KEY>
   - <CLOUD_PUBLIC_KEY>
   - <CLOUD_PUBLIC_IP>（云 EIP，如 8.x.x.x）
   - Endpoint 端口与 ListenPort 一致（默认 51820）

4) 云机：
   sudo cp wg0-cloud.conf.example /etc/wireguard/wg0.conf
   sudo chmod 600 /etc/wireguard/wg0.conf
   sudo sysctl -w net.ipv4.ip_forward=1
   echo 'net.ipv4.ip_forward=1' | sudo tee /etc/sysctl.d/99-wireguard-forward.conf
   sudo wg-quick up wg0
   sudo systemctl enable wg-quick@wg0

5) 内网机：
   sudo cp wg0-office.conf.example /etc/wireguard/wg0.conf
   sudo chmod 600 /etc/wireguard/wg0.conf
   sudo sysctl -w net.ipv4.ip_forward=1
   sudo wg-quick up wg0
   sudo systemctl enable wg-quick@wg0

6) 云机测试：
   ping -c 2 10.200.200.2
   ping -c 2 10.40.88.38
   curl -I --connect-timeout 5 http://10.40.88.38:8080/

   若 ping 不通 10.40.88.38：在 10.40.88.38 或网关上添加静态路由：
   目标 10.200.200.0/24 下一跳 = 内网 WireGuard 那台机的局域网 IP（10.40.88.x）。

7) 云机 Nginx：
   sudo apt install -y nginx
   sudo cp nginx-shuhe-proxy.conf /etc/nginx/sites-available/shuhe-proxy.conf
   sudo ln -sf /etc/nginx/sites-available/shuhe-proxy.conf /etc/nginx/sites-enabled/
   sudo rm -f /etc/nginx/sites-enabled/default
   sudo nginx -t && sudo systemctl reload nginx

对外访问：http://<云EIP>/ 与 http://<云EIP>:8080/（与 nginx 配置一致）

VPN 网段默认 10.200.200.0/24；若要改，云与内网两处 Address/AllowedIPs 需一起改。
