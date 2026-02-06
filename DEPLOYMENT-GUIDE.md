# 戍合管理系统 - 完整部署指南

---

## 🚀 部署前必做清单

在开始部署之前，请确保完成以下准备工作：

### ✅ 1. 数据库准备
- [ ] 在生产服务器安装 MySQL 8.0+
- [ ] 创建生产数据库：`CREATE DATABASE shuhe-ms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
- [ ] 创建专用数据库用户（不要用 root！）
- [ ] 导入备份数据或初始化脚本

### ✅ 2. Redis 准备
- [ ] 在生产服务器安装 Redis 6.0+
- [ ] 配置 Redis 密码（生产必须！）
- [ ] 修改 `bind` 配置（如需外部访问）

### ✅ 3. 服务器环境
- [ ] 安装 JDK 21+
- [ ] 安装 Nginx
- [ ] 开放必要端口（48080、80、443）
- [ ] 配置防火墙规则

### ✅ 4. 配置文件准备
- [ ] 设置环境变量（见下方环境变量说明）
- [ ] 或修改 `application-prod.yaml` 配置

---

## 🔧 生产环境配置

### 环境类型说明

| 环境 | 配置文件 | 用途 |
|------|---------|------|
| local | application-local.yaml | 本地开发 |
| dev | application-dev.yaml | 开发测试 |
| **prod** | **application-prod.yaml** | **生产环境** |

### 切换到生产环境

**启动时指定环境（推荐）：**
```bash
java -jar shuhe-server.jar --spring.profiles.active=prod
```

### 生产环境变量配置

在服务器上设置以下环境变量：

```bash
# ========== 数据库配置 ==========
export DB_HOST=192.168.1.100          # MySQL 地址
export DB_PORT=3306                   # MySQL 端口
export DB_NAME=shuhe-ms               # 数据库名
export DB_USERNAME=shuhe_prod         # 数据库用户名（不要用 root！）
export DB_PASSWORD=YourSecurePassword # 数据库密码

# ========== Redis 配置 ==========
export REDIS_HOST=192.168.1.100       # Redis 地址
export REDIS_PORT=6379                # Redis 端口
export REDIS_PASSWORD=RedisPassword   # Redis 密码（生产必须设置！）
export REDIS_DATABASE=0               # Redis 数据库索引

# ========== 监控配置 ==========
export DRUID_USERNAME=druid_admin     # Druid 监控用户名
export DRUID_PASSWORD=DruidPassword   # Druid 监控密码
export ADMIN_USERNAME=admin           # Spring Boot Admin 用户名
export ADMIN_PASSWORD=AdminPassword   # Spring Boot Admin 密码

# ========== 日志配置 ==========
export LOG_PATH=/var/log/shuhe        # 日志目录
```

### 创建生产数据库用户

```sql
-- 创建专用用户（不要用 root！）
CREATE USER 'shuhe_prod'@'%' IDENTIFIED BY 'YourSecurePassword';
GRANT ALL PRIVILEGES ON `shuhe-ms`.* TO 'shuhe_prod'@'%';
FLUSH PRIVILEGES;
```

---

## 📋 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue3)                            │
│                   端口: 80 / 8080                           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot)                        │
│                   端口: 48080                               │
└─────────────────────────────────────────────────────────────┘
        │                                       │
        ▼                                       ▼
┌───────────────────────┐         ┌───────────────────────────┐
│     MySQL 8.x         │         │       Redis 6.x           │
│   数据库: shuhe-ms    │         │     数据库索引: 1         │
│     端口: 3306        │         │       端口: 6379          │
└───────────────────────┘         └───────────────────────────┘
```

---

## 🗄️ 第一步：数据库备份

### 方式一：使用 mysqldump 命令行（推荐）

**在 MySQL 所在服务器执行：**

```bash
# 完整备份（包含存储过程、触发器、事件）
mysqldump -u root -p123456 \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  shuhe-ms > shuhe-ms_backup_$(date +%Y%m%d_%H%M%S).sql

# 如果 MySQL 在 Docker 容器中
docker exec shuhe-mysql mysqldump -u root -p123456 \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  shuhe-ms > shuhe-ms_backup_$(date +%Y%m%d_%H%M%S).sql
```

**Windows PowerShell 版本：**

```powershell
# 设置 MySQL bin 目录到 PATH（根据实际安装路径修改）
$env:Path += ";C:\Program Files\MySQL\MySQL Server 8.0\bin"

# 执行备份
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
mysqldump -u root -p123456 --single-transaction --routines --triggers --events shuhe-ms > "shuhe-ms_backup_$timestamp.sql"
```

### 方式二：使用图形化工具

1. **Navicat / DBeaver / DataGrip**：
   - 连接数据库 `shuhe-ms`
   - 右键 → 导出 / Dump → SQL 文件
   - 选择"包含结构和数据"

2. **phpMyAdmin**：
   - 选择数据库 `shuhe-ms`
   - 导出 → 自定义 → SQL → 执行

### 方式三：使用项目内置 SQL 文件（首次部署）

如果是全新部署，使用项目中的初始化 SQL：
- 完整初始化：`sql/mysql/shuhe-ms.sql`
- 生产环境初始化：`sql/mysql/shuhe-ms-production-init.sql`
- Quartz 定时任务表：`sql/mysql/quartz.sql`

---

## 🚀 第二步：选择部署方式

### 🐳 方式 A：Docker Compose 部署（推荐生产环境）

#### 1. 安装前置依赖

```bash
# 安装 Docker（Ubuntu/Debian）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo apt install docker-compose-plugin
```

#### 2. 准备部署文件

```bash
# 进入项目目录
cd /path/to/shu-he-management-system-backend

# 复制必要文件到服务器
# 需要的文件结构：
# deploy/
# ├── docker-compose.yml        (从 script/docker/ 复制)
# ├── docker.env                (从 script/docker/ 复制并修改)
# ├── shuhe-server/
# │   ├── Dockerfile
# │   └── target/
# │       └── shuhe-server.jar  (构建后的 JAR)
# └── shuhe-ui-admin/           (前端项目)
#     ├── Dockerfile
#     ├── nginx.conf
#     └── dist/                 (构建后的前端文件)
```

#### 3. 构建后端 JAR 包

```bash
# 使用 Maven 构建
mvn clean package -DskipTests

# 或者使用 Docker 构建（无需本地安装 Maven）
docker volume create --name shuhe-maven-repo
docker run -it --rm --name shuhe-maven \
    -v shuhe-maven-repo:/root/.m2 \
    -v $PWD:/usr/src/mymaven \
    -w /usr/src/mymaven \
    maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests
```

#### 4. 配置环境变量

编辑 `docker.env` 文件：

```env
## MySQL 配置
MYSQL_DATABASE=shuhe-ms
MYSQL_ROOT_PASSWORD=你的强密码

## 后端配置
JAVA_OPTS=-Xms1024m -Xmx2048m -Djava.security.egd=file:/dev/./urandom

MASTER_DATASOURCE_URL=jdbc:mysql://shuhe-mysql:3306/${MYSQL_DATABASE}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
MASTER_DATASOURCE_USERNAME=root
MASTER_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
SLAVE_DATASOURCE_URL=${MASTER_DATASOURCE_URL}
SLAVE_DATASOURCE_USERNAME=${MASTER_DATASOURCE_USERNAME}
SLAVE_DATASOURCE_PASSWORD=${MASTER_DATASOURCE_PASSWORD}
REDIS_HOST=shuhe-redis

## 前端配置
NODE_ENV=production
PUBLIC_PATH=/
VUE_APP_TITLE=戍合管理系统
VUE_APP_BASE_API=/prod-api
VUE_APP_TENANT_ENABLE=true
VUE_APP_CAPTCHA_ENABLE=true
```

#### 5. 修改 docker-compose.yml（生产环境优化）

```yaml
version: "3.4"
name: shuhe-system

services:
  mysql:
    container_name: shuhe-mysql
    image: mysql:8
    restart: always
    ports:
      - "3306:3306"  # 生产环境建议只绑定内网 IP
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE:-shuhe-ms}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      TZ: Asia/Shanghai
    volumes:
      - mysql_data:/var/lib/mysql/
      - ./init-sql:/docker-entrypoint-initdb.d:ro  # 初始化 SQL 目录
    command: 
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-time-zone=+08:00
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    container_name: shuhe-redis
    image: redis:6-alpine
    restart: always
    ports:
      - "6379:6379"  # 生产环境建议只绑定内网 IP
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-}
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  server:
    container_name: shuhe-server
    build:
      context: ./shuhe-server/
    image: shuhe-server:latest
    restart: always
    ports:
      - "48080:48080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JAVA_OPTS: ${JAVA_OPTS:-"-Xms512m -Xmx1024m"}
      ARGS: >-
        --spring.datasource.dynamic.datasource.master.url=${MASTER_DATASOURCE_URL}
        --spring.datasource.dynamic.datasource.master.username=${MASTER_DATASOURCE_USERNAME}
        --spring.datasource.dynamic.datasource.master.password=${MASTER_DATASOURCE_PASSWORD}
        --spring.data.redis.host=${REDIS_HOST:-shuhe-redis}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:48080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  admin:
    container_name: shuhe-admin
    build:
      context: ./shuhe-ui-admin
    image: shuhe-admin:latest
    restart: always
    ports:
      - "80:80"
    depends_on:
      - server

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local
```

#### 6. 启动服务

```bash
# 首次启动（会自动构建镜像）
docker compose --env-file docker.env up -d

# 查看日志
docker compose logs -f

# 查看服务状态
docker compose ps

# 停止服务
docker compose down

# 重新构建并启动
docker compose --env-file docker.env up -d --build
```

---

### 📦 方式 B：传统 JAR 部署

#### 1. 服务器环境准备

```bash
# 安装 JDK 21
sudo apt install openjdk-21-jdk

# 安装 MySQL 8
sudo apt install mysql-server

# 安装 Redis
sudo apt install redis-server

# 验证安装
java -version
mysql --version
redis-cli ping
```

#### 2. 配置 MySQL

```bash
# 登录 MySQL
sudo mysql -u root -p

# 创建数据库
CREATE DATABASE `shuhe-ms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（生产环境）
CREATE USER 'shuhe'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON `shuhe-ms`.* TO 'shuhe'@'localhost';
FLUSH PRIVILEGES;

# 导入数据
mysql -u root -p shuhe-ms < shuhe-ms.sql
mysql -u root -p shuhe-ms < quartz.sql
```

#### 3. 部署目录结构

```bash
# 创建部署目录
sudo mkdir -p /work/projects/shuhe-server/{build,backup,heapError,logs}
sudo chown -R $USER:$USER /work/projects/shuhe-server

# 目录结构
/work/projects/shuhe-server/
├── shuhe-server.jar          # 当前运行的 JAR
├── build/                    # Jenkins 上传的新 JAR
│   └── shuhe-server.jar
├── backup/                   # JAR 备份目录
├── heapError/                # 堆内存溢出 dump
└── logs/                     # 日志目录
```

#### 4. 创建 systemd 服务

```bash
sudo vim /etc/systemd/system/shuhe-server.service
```

```ini
[Unit]
Description=Shuhe Management System Backend
After=network.target mysql.service redis.service

[Service]
Type=simple
User=shuhe
WorkingDirectory=/work/projects/shuhe-server
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/work/projects/shuhe-server/heapError"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /work/projects/shuhe-server/shuhe-server.jar --spring.profiles.active=dev
ExecStop=/bin/kill -15 $MAINPID
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启用并启动服务
sudo systemctl daemon-reload
sudo systemctl enable shuhe-server
sudo systemctl start shuhe-server

# 查看状态
sudo systemctl status shuhe-server

# 查看日志
sudo journalctl -u shuhe-server -f
```

#### 5. 或使用部署脚本

项目已提供部署脚本 `script/shell/deploy.sh`，修改配置后使用：

```bash
# 修改脚本中的配置
vim script/shell/deploy.sh

# 主要配置项：
# BASE_PATH=/work/projects/shuhe-server
# PROFILES_ACTIVE=dev  # 或 production
# JAVA_OPS="-Xms512m -Xmx1024m ..."

# 执行部署
chmod +x deploy.sh
./deploy.sh
```

---

### 🌐 方式 C：Nginx 反向代理配置

#### 完整 Nginx 配置

```nginx
# /etc/nginx/sites-available/shuhe.conf

upstream shuhe-backend {
    server 127.0.0.1:48080 weight=5 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/your-domain.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.key;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    # 前端静态文件
    location / {
        root /var/www/shuhe-admin/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
        
        # 缓存静态资源
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
            expires 30d;
            add_header Cache-Control "public, no-transform";
        }
    }

    # API 反向代理
    location /prod-api/ {
        proxy_pass http://shuhe-backend/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 上传文件大小限制
        client_max_body_size 100m;
    }

    # Actuator 端点（仅内网访问）
    location /actuator/ {
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        allow 172.16.0.0/12;
        allow 192.168.0.0/16;
        deny all;
        
        proxy_pass http://shuhe-backend/actuator/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Druid 监控（仅内网访问）
    location /druid/ {
        allow 127.0.0.1;
        allow 10.0.0.0/8;
        deny all;
        
        proxy_pass http://shuhe-backend/druid/;
        proxy_set_header Host $host;
    }

    # 日志配置
    access_log /var/log/nginx/shuhe-access.log;
    error_log /var/log/nginx/shuhe-error.log;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
}
```

---

## ✅ 第三步：部署检查清单

### 部署前检查

- [ ] 数据库已备份
- [ ] 生产环境配置文件已准备（修改密码、URL等）
- [ ] SSL 证书已准备（如需 HTTPS）
- [ ] 服务器防火墙端口已开放（80, 443, 48080）
- [ ] 域名 DNS 已解析

### 部署后验证

```bash
# 1. 检查服务状态
docker compose ps
# 或
systemctl status shuhe-server

# 2. 检查健康端点
curl http://localhost:48080/actuator/health

# 3. 检查数据库连接
curl http://localhost:48080/actuator/health/db

# 4. 检查 Redis 连接
curl http://localhost:48080/actuator/health/redis

# 5. 测试登录接口
curl -X POST http://localhost:48080/admin-api/system/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 超级管理员 |

---

## 🔧 第四步：运维操作

### 日志查看

```bash
# Docker 方式
docker logs -f shuhe-server --tail 100

# JAR 方式
tail -f ~/logs/shuhe-server.log
# 或
journalctl -u shuhe-server -f
```

### 数据库定时备份脚本

创建 `/opt/scripts/backup-mysql.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="shuhe-ms"
DB_USER="root"
DB_PASS="your_password"

mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u$DB_USER -p$DB_PASS \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME | gzip > $BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: ${DB_NAME}_${DATE}.sql.gz"
```

添加定时任务：

```bash
# 每天凌晨 3 点执行备份
crontab -e
0 3 * * * /opt/scripts/backup-mysql.sh >> /var/log/mysql-backup.log 2>&1
```

### 服务重启

```bash
# Docker 方式
docker compose restart server

# Systemd 方式
sudo systemctl restart shuhe-server
```

---

## ⚠️ 生产环境安全建议

1. **修改所有默认密码**
   - MySQL root 密码
   - Redis 密码
   - 管理员 admin 密码

2. **限制端口访问**
   - MySQL 3306 和 Redis 6379 只允许内网访问
   - 后端 48080 通过 Nginx 代理，不直接暴露

3. **启用 HTTPS**
   - 使用 Let's Encrypt 免费证书
   - 强制 HTTP 重定向到 HTTPS

4. **配置防火墙**
   ```bash
   # UFW 示例
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw deny 3306/tcp
   sudo ufw deny 6379/tcp
   sudo ufw deny 48080/tcp
   sudo ufw enable
   ```

5. **定期备份**
   - 数据库每日备份
   - 备份文件异地存储

---

## 📞 常见问题

### Q1: 启动失败，提示数据库连接错误
检查：
- MySQL 服务是否启动
- 数据库名、用户名、密码是否正确
- 防火墙是否阻止连接

### Q2: 前端无法访问后端 API
检查：
- Nginx 反向代理配置是否正确
- 后端服务是否正常运行
- CORS 配置是否正确

### Q3: 内存不足
调整 JVM 参数：
```bash
JAVA_OPTS="-Xms256m -Xmx512m"  # 小内存服务器
JAVA_OPTS="-Xms1g -Xmx2g"      # 推荐配置
```

---

**部署完成后，访问 http://your-domain.com 即可使用系统！**
