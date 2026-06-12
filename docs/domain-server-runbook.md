# 真实域名服务器上线 Runbook

创建日期：2026-06-12

本文档用于把五行人格卡从“公网 IP 可访问”推进到“真实域名可访问”。它假设应用仓库在服务器：

```text
/opt/wuxing-persona-card
```

如果服务器无法从 GitHub 拉取代码，可以先在本地推送到 `origin/main`，再用压缩包或单文件上传方式同步；不要因为服务器 GitHub 网络抖动误判为应用故障。

## 1. 上线拓扑

推荐拓扑：

```text
Browser
  -> DNS A record
  -> Server public IP
  -> Host Nginx :80/:443
  -> Docker Compose nginx 127.0.0.1:8088
  -> Spring Boot backend / MySQL / Redis
```

为什么这么做：

- 宿主机 Nginx 负责证书、HTTPS、HSTS 和公网入口。
- 容器 Nginx 继续负责 Vue 静态资源、`/api/**` 和 `/s/**` 转发。
- MySQL、Redis、Spring Boot 仍留在 Docker 内网，不暴露公网。

## 2. DNS 和安全组

在 DNS 服务商添加：

```text
主机记录：<主域名前缀>
记录类型：A
记录值：<服务器公网 IP>
TTL：默认或 600 秒
```

服务器安全组 / 防火墙确认：

```text
80/tcp  open
443/tcp open
22/tcp  open only for SSH
```

服务器上可检查：

```bash
sudo ufw status
```

## 3. 准备应用容器入口

进入服务器仓库：

```bash
cd /opt/wuxing-persona-card
git status -sb
git pull --ff-only origin main
```

如果服务器拉取 GitHub 超时，先记录日志，再改用上传同步，不要阻塞域名排查。

编辑 `deploy/.env`，首次真实域名上线推荐：

```text
APP_BASE_URL=https://<主域名>
SHORT_LINK_MODE=internal
NGINX_HTTP_PORT=127.0.0.1:8088
ADMIN_TOKEN=<strong-random-token>
HASH_SALT=<strong-random-salt>
MYSQL_PASSWORD=<strong-password>
MYSQL_ROOT_PASSWORD=<strong-password>
VISIT_EVENT_ASYNC_QUEUE_CAPACITY=2048
VISIT_EVENT_ASYNC_DRAIN_LIMIT=64
SHORT_LINK_LAST_VISIT_TOUCH_INTERVAL_SECONDS=30
```

说明：

- `APP_BASE_URL` 必须是用户实际访问域名，否则结果页分享链接会错。
- `NGINX_HTTP_PORT=127.0.0.1:8088` 让容器入口只监听本机，公网只暴露宿主机 Nginx。
- `SHORT_LINK_MODE=internal` 表示 `/s/{code}` 仍由五行后端处理，减少首次上线变量。

预检并启动：

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

服务器本机验证容器入口：

```bash
curl -fsS http://127.0.0.1:8088/api/health
curl -fsS http://127.0.0.1:8088/api/questions
```

## 4. 安装宿主机 Nginx 和证书工具

Ubuntu 示例：

```bash
sudo apt-get update
sudo apt-get install -y nginx certbot python3-certbot-nginx
sudo mkdir -p /var/www/certbot
```

首次签发证书前，先启用一个临时 HTTP 站点。不要直接启用 `deploy/host-nginx-domain-tls.example.conf`，因为它引用的证书文件还不存在，`nginx -t` 会失败。

临时配置内容：

```nginx
server {
    listen 80;
    server_name <主域名>;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto http;
    }
}
```

保存到：

```text
/etc/nginx/sites-available/wuxing-persona-card.conf
```

然后启用并检查：

```bash
sudo ln -sf /etc/nginx/sites-available/wuxing-persona-card.conf /etc/nginx/sites-enabled/wuxing-persona-card.conf
sudo nginx -t
sudo systemctl reload nginx
```

临时 HTTP 只用于首绑和签证书，不适合长期公开。

## 5. 签发 HTTPS 证书

确保 DNS 已解析到服务器后：

```bash
sudo certbot certonly --webroot -w /var/www/certbot -d <主域名>
```

证书签发成功后，再启用仓库里的 HTTPS 模板：

```bash
sudo cp /opt/wuxing-persona-card/deploy/host-nginx-domain-tls.example.conf \
  /etc/nginx/sites-available/wuxing-persona-card.conf
sudo sed -i 's/wuxing.example.com/<主域名>/g' /etc/nginx/sites-available/wuxing-persona-card.conf
```

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://<主域名>/
sudo certbot renew --dry-run
```

看到 `HTTP/2 200` 或 `HTTP/1.1 200`，并且有 `strict-transport-security`，说明 HTTPS 入口基本可用。

## 6. 域名和业务验证

从本地电脑执行：

```bash
DOMAIN=<主域名> \
EXPECTED_IP=<服务器公网 IP> \
BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
scripts/domain-bind-preflight.sh
```

生产主链路 smoke：

```bash
BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
scripts/production-smoke-test.sh
```

低延迟和异步排水 smoke：

```bash
BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
SHORTLINK_HITS=30 \
ADMIN_HITS=2 \
MAX_ASYNC_QUEUE_SIZE=0 \
MAX_ASYNC_DROPPED_EVENTS=0 \
MAX_ASYNC_BATCH_FAILURES=0 \
scripts/performance-smoke-test.sh
```

浏览器验证：

- 打开 `https://<主域名>/`。
- 完成一次测试。
- 在结果页点击“复制分享链接”。
- 新窗口打开分享链接，确认能进入同一张结果页。
- 打开 `https://<主域名>/admin`，用 `ADMIN_TOKEN` 登录，确认 overview 有数据。

## 7. 回滚

如果域名入口失败，但容器本机入口可用：

1. 先回滚宿主机 Nginx 配置。
2. 保留 Docker 容器运行，避免误伤应用。
3. 用 `curl http://127.0.0.1:8088/api/health` 区分是 Nginx/TLS 问题还是应用问题。

如果应用新版本失败：

```bash
cd /opt/wuxing-persona-card
TARGET_REF=<last-good-ref> ENV_FILE=deploy/.env scripts/deploy-rollback.sh
```

## 8. 学习口径

> 真实域名上线不是只做 DNS。DNS 只是把域名指向服务器；宿主机 Nginx 负责 80/443 和证书；容器 Nginx 负责应用路由；`APP_BASE_URL` 决定新生成的分享链接域名；production smoke 证明业务链路可用；performance smoke 再看短链 P95、后台 P95 和异步事件排水。
