# 真实域名上线实时状态

更新日期：2026-06-12

本文档记录 `wuxingcard.com` 真实域名上线过程中的当前事实、阻塞点和下一步执行命令。它不是长期架构文档，而是为了让后续接力不丢状态。

## 1. 当前已知信息

| 项目 | 当前值 |
| --- | --- |
| 主域名 | `wuxingcard.com` |
| www 域名 | `www.wuxingcard.com` |
| 服务器公网 IP | `82.157.137.36` |
| 服务器 SSH | `ubuntu@82.157.137.36` |
| 服务器仓库路径 | `/opt/wuxing-persona-card` |
| DNS 服务商 | 腾讯云 / DNSPod |
| DNS A 记录 | 注册时已勾选快速解析，`@` 和 `www` 指向 `82.157.137.36` |
| HTTPS 方案 | 首发建议 Certbot |
| 短链子域名 | 首发暂不启用，保持 `SHORT_LINK_MODE=internal` |
| 密钥/密码生成 | 用户允许 Codex 在服务器本地生成 |
| 临时 HTTP 验证 | 用户允许 |

## 2. 当前状态

- 域名 `wuxingcard.com` 已购买 1 年，处于注册/实名审核阶段。
- 本地 DNS 检查暂未解析到公网 IP，符合刚注册后等待审核和解析生效的状态。
- SSH 授权请求在当前 Codex 执行环境里未完成审批，因此还没有登录服务器执行变更。
- 仓库内前置准备已完成：域名自审、信息清单、宿主机 Nginx/TLS 模板、服务器 runbook 和学习手册均已落盘。

## 3. 等待 DNS 生效时的检查命令

从本地仓库执行：

```bash
DOMAIN=wuxingcard.com \
EXPECTED_IP=82.157.137.36 \
scripts/domain-dns-readiness.sh
```

如果只想先检查根域名，不检查 `www`：

```bash
DOMAIN=wuxingcard.com \
EXPECTED_IP=82.157.137.36 \
CHECK_WWW=false \
scripts/domain-dns-readiness.sh
```

通过条件：

```text
wuxingcard.com      -> 82.157.137.36
www.wuxingcard.com  -> 82.157.137.36
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
APP_BASE_URL=http://wuxingcard.com
SHORT_LINK_MODE=internal
NGINX_HTTP_PORT=127.0.0.1:8088
```

首次临时 HTTP 验证可先用 `http://wuxingcard.com`；签发 HTTPS 后再改成：

```text
APP_BASE_URL=https://wuxingcard.com
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
curl -I http://wuxingcard.com/
ALLOW_HTTP=true DOMAIN=wuxingcard.com BASE_URL=http://wuxingcard.com scripts/domain-bind-preflight.sh
```

6. DNS、HTTP 和备案/接入状态允许后，再用 Certbot 签发 HTTPS，并切换到 `deploy/host-nginx-domain-tls.example.conf`。

## 5. 学习口径

> 当前阶段不是“代码还没准备好”，而是“外部域名系统仍在生效中”。真实域名上线要依次证明：域名已实名/审核通过、DNS A 记录指向服务器、服务器 80/443 开放、宿主机 Nginx 能接流量、容器 Nginx 只监听本机、`APP_BASE_URL` 与用户访问域名一致、业务 smoke 和性能 smoke 都通过。
