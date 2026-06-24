# 真实域名上线实时状态

更新日期：2026-06-24

本文档记录 `wuxingcard.cn` 真实域名上线过程中的当前事实、阻塞点和下一步执行命令。它不是长期架构文档，而是为了让后续接力不丢状态。

## 1. 当前已知信息

| 项目 | 当前值 |
| --- | --- |
| 主域名 | `wuxingcard.cn` |
| www 域名 | `www.wuxingcard.cn` |
| 服务器公网 IP | `82.157.137.36` |
| 服务器 SSH | `ubuntu@82.157.137.36` |
| 服务器仓库路径 | `/opt/wuxing-persona-card` |
| DNS 服务商 | 腾讯云 / DNSPod |
| DNS A 记录 | `@` 和 `www` 已指向 `82.157.137.36`，本地预检通过 |
| HTTPS 方案 | Certbot / Let's Encrypt 已签发并启用 |
| 短链子域名 | 首发暂不启用，保持 `SHORT_LINK_MODE=internal` |
| 备案状态 | 备案订单已通过管局审核，正式备案号待短信/邮箱下发 |
| 密钥/密码生成 | 用户允许 Codex 在服务器本地生成 |
| 当前公网入口 | `https://wuxingcard.cn`、`https://www.wuxingcard.cn` |

## 2. 当前状态

- 域名 `wuxingcard.cn` 已注册成功，DNS A 记录已生效。
- `wuxingcard.cn` 的 NS 已能查到 DNSPod：`brandy.dnspod.net`、`seventy.dnspod.net`。
- `http://wuxingcard.cn` 已返回 `301`，重定向到 `https://wuxingcard.cn/`。
- `https://wuxingcard.cn` 和 `https://www.wuxingcard.cn` 均已返回 `HTTP/2 200`，并带有 `strict-transport-security`。
- 服务器已通过临时 SSH 公钥登录，代码已快进到 `9060df7`。
- 服务器 `deploy/.env` 已切换为 `APP_BASE_URL=https://wuxingcard.cn`、`SHORT_LINK_MODE=internal`、`NGINX_HTTP_PORT=127.0.0.1:8088`。
- 宿主机 Nginx 监听公网 `80/443`，容器 Nginx 仅监听本机 `127.0.0.1:8088`。
- Let's Encrypt 证书路径为 `/etc/letsencrypt/live/wuxingcard.cn/fullchain.pem`，证书有效期到 `2026-09-10`。
- Docker Compose 已使用最新代码重建并启动，`scripts/production-smoke-test.sh` 在服务器本机通过，样例 `resultId=R20260612033152184633`、`shortCode=IEUReb`。
- `DOMAIN=wuxingcard.cn EXPECTED_IP=82.157.137.36 scripts/domain-dns-readiness.sh` 已通过。
- `DOMAIN=wuxingcard.cn BASE_URL=https://wuxingcard.cn scripts/domain-bind-preflight.sh` 已通过。
- 真实域名 production smoke 已通过，样例 `resultId=R20260612034943137920`、`shortCode=yEWIdP`。
- HTTPS 真实域名 production smoke 已通过，样例 `resultId=R20260612043129993655`、`shortCode=h7KjpJ`。
- 仓库内前置准备已完成：域名自审、信息清单、宿主机 Nginx/TLS 模板、服务器 runbook 和学习手册均已落盘。
- 2026-06-24 用户确认腾讯云备案订单已通过管局审核；正式备案号尚需等通信管理局短信/邮箱下发，不能用备案订单号替代。
- 仓库已补备案页脚配置：`VITE_ICP_RECORD_NO` 控制展示正式备案号，`VITE_ICP_LINK` 默认链接 `https://beian.miit.gov.cn/`；`wuxingcard.cn` 正式域名预检会要求备案号已配置。

## 3. 当前复查命令

从本地仓库执行：

```bash
DOMAIN=wuxingcard.cn EXPECTED_IP=82.157.137.36 scripts/domain-dns-readiness.sh
```

HTTPS 入口和业务基础预检：

```bash
DOMAIN=wuxingcard.cn BASE_URL=https://wuxingcard.cn scripts/domain-bind-preflight.sh
```

服务器生产 smoke：

```bash
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=<admin-token> scripts/production-smoke-test.sh
```

备案号下发后的前端重建：

```bash
cd /opt/wuxing-persona-card
ICP_RECORD_NO='<通信管理局下发的正式备案号>' \
BASE_URL=https://wuxingcard.cn \
NGINX_HTTP_PORT_VALUE=127.0.0.1:8088 \
APPLY_COMPOSE=true \
scripts/set-production-entry.sh
```

## 4. DNS 生效后的服务器执行顺序

1. 登录服务器：

```bash
ssh ubuntu@82.157.137.36
```

2. 确认仓库和运行环境：

```bash
cd /opt/wuxing-persona-card
git status -sb
git pull --ff-only origin main
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
sudo ufw status
```

3. 更新服务器本地 `deploy/.env`，建议值：

```text
APP_BASE_URL=http://wuxingcard.cn
SHORT_LINK_MODE=internal
NGINX_HTTP_PORT=127.0.0.1:8088
```

首次临时 HTTP 验证可先用 `http://wuxingcard.cn`；签发 HTTPS 后再改成：

```text
APP_BASE_URL=https://wuxingcard.cn
VITE_ICP_RECORD_NO=<通信管理局下发的正式备案号>
VITE_ICP_LINK=https://beian.miit.gov.cn/
```

4. 重启 Compose：

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
curl -fsS http://127.0.0.1:8088/api/health
```

5. 配置宿主机 Nginx 临时 HTTP 站点并验证：

```bash
curl -I http://wuxingcard.cn/
ALLOW_HTTP=true DOMAIN=wuxingcard.cn BASE_URL=http://wuxingcard.cn scripts/domain-bind-preflight.sh
```

6. DNS、HTTP 和备案/接入状态允许后，再用 Certbot 签发 HTTPS，并切换到 `deploy/host-nginx-domain-tls.example.conf`。

## 5. 学习口径

> 当前线上拓扑是：DNS A 记录把 `wuxingcard.cn` 指向服务器，宿主机 Nginx 负责 `80 -> 443`、TLS 和安全响应头，容器 Nginx 只监听 `127.0.0.1:8088`，`APP_BASE_URL=https://wuxingcard.cn` 决定新生成结果和分享链接的真实公网域名。后续继续做线上压测、告警和备份恢复时，要以这个 HTTPS 入口为准。
