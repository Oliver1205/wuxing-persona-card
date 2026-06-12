# 真实域名上线实时状态

更新日期：2026-06-12

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
| HTTPS 方案 | 首发建议 Certbot |
| 短链子域名 | 首发暂不启用，保持 `SHORT_LINK_MODE=internal` |
| 密钥/密码生成 | 用户允许 Codex 在服务器本地生成 |
| 临时 HTTP 验证 | 用户允许 |

## 2. 当前状态

- 域名 `wuxingcard.cn` 已注册成功，DNS A 记录已生效。
- `wuxingcard.cn` 的 NS 已能查到 DNSPod：`brandy.dnspod.net`、`seventy.dnspod.net`。
- `http://82.157.137.36` 已能从公网访问，首页返回 `HTTP/1.1 200 OK`。
- 服务器已通过临时 SSH 公钥登录，代码已快进到 `9060df7`。
- 服务器 `deploy/.env` 已切换为 `APP_BASE_URL=http://wuxingcard.cn`、`SHORT_LINK_MODE=internal`、`NGINX_HTTP_PORT=80`。
- Docker Compose 已使用最新代码重建并启动，`scripts/production-smoke-test.sh` 在服务器本机通过，样例 `resultId=R20260612033152184633`、`shortCode=IEUReb`。
- `DOMAIN=wuxingcard.cn EXPECTED_IP=82.157.137.36 scripts/domain-dns-readiness.sh` 已通过。
- `ALLOW_HTTP=true DOMAIN=wuxingcard.cn BASE_URL=http://wuxingcard.cn scripts/domain-bind-preflight.sh` 已通过。
- `ALLOW_HTTP=true DOMAIN=www.wuxingcard.cn BASE_URL=http://www.wuxingcard.cn scripts/domain-bind-preflight.sh` 已通过。
- 真实域名 production smoke 已通过，样例 `resultId=R20260612034943137920`、`shortCode=yEWIdP`。
- 仓库内前置准备已完成：域名自审、信息清单、宿主机 Nginx/TLS 模板、服务器 runbook 和学习手册均已落盘。

## 3. 等待 DNS 生效时的检查命令

从本地仓库执行：

```bash
DOMAIN=wuxingcard.cn \
EXPECTED_IP=82.157.137.36 \
scripts/domain-dns-readiness.sh
```

如果只想先检查根域名，不检查 `www`：

```bash
DOMAIN=wuxingcard.cn \
EXPECTED_IP=82.157.137.36 \
CHECK_WWW=false \
scripts/domain-dns-readiness.sh
```

通过条件：

```text
wuxingcard.cn      -> 82.157.137.36
www.wuxingcard.cn  -> 82.157.137.36
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

> 当前阶段不是“代码还没准备好”，而是“外部域名系统仍在生效中”。真实域名上线要依次证明：域名已实名/审核通过、DNS A 记录指向服务器、服务器 80/443 开放、宿主机 Nginx 能接流量、容器 Nginx 只监听本机、`APP_BASE_URL` 与用户访问域名一致、业务 smoke 和性能 smoke 都通过。
