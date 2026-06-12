# 真实域名上线前严格自审

审查日期：2026-06-12

本文档用于五小时域名上线工作流的第一阶段：先自审，再绑定真实域名。结论采用面试和上线都能使用的口径：不夸大生产能力，先找会影响真实访问、分享、数据和安全边界的问题。

## 1. 总体结论

当前项目已经适合做真实域名下的公开演示，但建议先走“主域名 + internal 短链”的低变量上线：

```text
https://<主域名>/          -> H5 首页 / 测试 / 结果 / 后台页面
https://<主域名>/api/**    -> Spring Boot API
https://<主域名>/s/{code} -> 内置短链跳转
```

独立短链子域名和 external 短链服务建议放到第二阶段。原因是主域名、HTTPS、线上 smoke、后台 token、备份恢复都还需要先跑通；此时再引入 external 服务会把风险面扩大到跨系统一致性和双域名证书。

## 2. P1 必须上线前确认

| 风险 | 位置 | 影响 | 建议动作 |
| --- | --- | --- | --- |
| 默认部署只暴露 HTTP 80，没有 TLS/HSTS 实装 | `deploy/nginx.conf`、`deploy/docker-compose.yml` | 真实域名可访问但浏览器会显示非 HTTPS；分享和后台 token 在公网下不够稳妥 | 先决定 TLS 方案：云厂商/CDN 证书、宿主机 Nginx + Certbot、或容器内 TLS。正式宣传前必须用 HTTPS |
| `.env` 里的 `APP_BASE_URL` 必须等于真实访问域名 | `deploy/.env.example`、`AppProperties` | 结果页返回的分享链接会写死错误域名，后续所有短链都带错地址 | 上线前设置 `APP_BASE_URL=https://<主域名>`，并执行 `scripts/deploy-preflight.sh deploy/.env` |
| 后台仍是单 token 管理保护 | `AdminController`、`AdminDashboard.vue` | `/admin` 公开后，弱 token 或泄露 token 会暴露运营数据和导出能力 | 使用长随机 `ADMIN_TOKEN`；不要在截图、文档和聊天中暴露；后续再规划 RBAC 或 Basic Auth/IP 白名单 |
| 域名/DNS/服务器权限当前不在仓库内 | 外部资源 | Codex 无法凭本地代码完成真实 DNS 绑定 | 需要用户提供主域名、DNS 管理入口或解析记录权限、服务器 SSH 入口、公网 IP、是否已有证书 |
| 中国大陆云服务器上的域名访问可能涉及服务商备案/接入检查 | 云厂商控制台 | DNS 解析成功也可能因为接入策略、备案、证书或安全组导致访问失败 | 在腾讯云控制台确认域名、备案/接入、80/443 安全组和证书状态 |

## 3. P2 建议本轮一起补强

| 风险 | 位置 | 影响 | 建议动作 |
| --- | --- | --- | --- |
| 域名绑定没有专门预检脚本 | `scripts/production-smoke-test.sh` 只能测业务链路 | DNS 是否解析到目标 IP、HTTP/HTTPS 是否通、后台 token 是否可用需要分散手测 | 已新增 `scripts/domain-bind-preflight.sh`，用于解析和基础健康检查 |
| 部署文档还有本地示例和旧分享措辞 | `docs/deploy.md` | 上线执行时容易把 `change-me`、localhost 或“复制短链接”带进真实环境 | 本轮应更新部署文档，把真实域名执行顺序和新脚本写清楚 |
| external 短链模式不适合和主域名首次上线同时做 | `ExternalShortLinkProvider`、`deploy/docker-compose.external-mode.yml` | 跨系统创建成功但本地绑定失败会进入人工补偿边界，增加首发复杂度 | 首次真实域名上线保持 `SHORT_LINK_MODE=internal`，后续再做 `s.<domain>` |
| 创建结果链路仍有同步事件写入 | `ResultService#create` | DB 短暂忙时虽然会降级日志，但创建事务内仍做多次事件记录，峰值下写放大存在 | 首发可接受；后续可以把非强证据事件继续异步化或用 outbox 统一治理 |
| 生产压测还没有真实域名证据 | `scripts/performance-smoke-test.sh`、`docs/production-load-alert-runbook.md` | 不能声称“生产高并发已验证” | 域名上线后跑 production smoke，再跑带阈值 performance smoke，记录 P95、队列、丢弃和失败数 |

## 4. P3 后续优化

- `/admin` 前端把 token 存在 `localStorage`，演示环境可接受；正式运营建议改为更完整的后台认证。
- `deploy/nginx.shortlink-routing.example.conf` 是示例，不含当前主 Nginx 的限流和安全响应头；如果以后复制使用，要同步限流和 header。
- `docs/deploy.md` 的章节编号有两个 `## 8`，不影响部署，但后续整理文档时应修正。
- `production-smoke-test.sh` 会创建真实结果数据；正式环境重复执行会产生测试数据，需要用固定 channel/campaign 标识并在后台识别。

## 5. 推荐本轮上线顺序

1. 用户提供主域名和公网服务器信息。
2. DNS 添加 `A` 记录：`<主域名> -> <服务器公网 IP>`。
3. 服务器安全组开放 `80` 和 `443`。
4. 服务器 `/opt/wuxing-persona-card` 同步到 `origin/main`。
5. 生成或更新 `deploy/.env`：

```text
APP_BASE_URL=https://<主域名>
SHORT_LINK_MODE=internal
NGINX_HTTP_PORT=127.0.0.1:8088
ADMIN_TOKEN=<strong-random-token>
HASH_SALT=<strong-random-salt>
MYSQL_PASSWORD=<strong-password>
MYSQL_ROOT_PASSWORD=<strong-password>
```

6. 容器 Nginx 先跑在 `127.0.0.1:8088`，外层宿主机 Nginx/证书负责 80/443；服务器执行步骤见 `docs/domain-server-runbook.md`。
7. 执行：

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
DOMAIN=<主域名> EXPECTED_IP=<服务器公网 IP> BASE_URL=https://<主域名> ADMIN_TOKEN=<token> scripts/domain-bind-preflight.sh
BASE_URL=https://<主域名> ADMIN_TOKEN=<token> scripts/production-smoke-test.sh
BASE_URL=https://<主域名> ADMIN_TOKEN=<token> SHORTLINK_HITS=30 \
MAX_ASYNC_QUEUE_SIZE=0 MAX_ASYNC_DROPPED_EVENTS=0 MAX_ASYNC_BATCH_FAILURES=0 \
scripts/performance-smoke-test.sh
```

## 6. 需要用户提供的信息

| 信息 | 用途 |
| --- | --- |
| 主域名，例如 `wuxing.example.com` | 设置 `APP_BASE_URL`、DNS 和 smoke |
| 是否需要短链子域名，例如 `s.example.com` | 决定是否启用 external/子域名阶段 |
| 服务器公网 IP | 校验 DNS 是否解析到正确目标 |
| 服务器 SSH 入口 | 进入 `/opt/wuxing-persona-card` 同步代码和重启服务 |
| DNS 服务商或控制台权限 | 添加/修改 A 记录 |
| HTTPS 证书方案 | 决定使用云证书、Certbot，还是先临时 HTTP |
| 真实 `ADMIN_TOKEN` 是否由用户保管 | 防止 token 出现在文档、提交和日志里 |

## 7. 面试讲法

> 我没有直接把本地 Docker Compose 暴露到公网就算上线，而是先做了域名上线前自审：确认主域名、APP_BASE_URL、DNS、安全组、HTTPS、后台 token、短链模式和 smoke 证据。首次真实域名上线我会先保持 internal 短链，减少跨系统变量；等主域名和 HTTPS 稳定，再扩展到独立短链子域名和 external 服务。
