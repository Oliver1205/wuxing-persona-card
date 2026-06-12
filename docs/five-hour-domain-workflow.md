# 五小时真实域名上线工作流

创建日期：2026-06-12

目标：在不破坏当前五行人格卡功能的前提下，先完成严格自审，再把项目推进到真实域名可访问，并继续补齐上线后的验证和学习沉淀。

## 1. 工作原则

- 先审查，后上线；不把“能访问”误当成“生产级”。
- 首次真实域名上线优先使用 `SHORT_LINK_MODE=internal`，减少外部短链服务变量。
- 任何真实 token、数据库密码、证书私钥都不写入 Git。
- 线上验证要保留命令和结果，不只看浏览器能打开。
- 工作完成后同步更新学习文档，让用户能讲清楚每一步为什么做。

## 2. 五小时安排

| 时间段 | 目标 | 产出 | 验证 |
| --- | --- | --- | --- |
| 0:00-0:45 | 严格自审 | `docs/domain-launch-self-audit.md` | 发现 P1/P2/P3 风险并明确首发边界 |
| 0:45-1:20 | 域名绑定准备 | DNS/服务器/证书信息清单，`scripts/domain-bind-preflight.sh` | 脚本语法检查，确认缺失外部信息 |
| 1:20-2:20 | 服务器和 `.env` 准备 | 服务器代码同步、`deploy/.env` 更新、Compose 重启 | `scripts/deploy-preflight.sh`、`docker compose config` |
| 2:20-3:20 | DNS 和 HTTPS | 主域名解析、宿主机 Nginx、80/443 入口、证书或临时 HTTP 决策 | `DOMAIN=... scripts/domain-bind-preflight.sh` |
| 3:20-4:10 | 线上主链路 smoke | 首页、题目、创建结果、短链跳转、后台 overview | `scripts/production-smoke-test.sh` |
| 4:10-4:40 | 低延迟和排水证据 | 短链 P95、后台 P95、异步队列/丢弃/批量失败 | `scripts/performance-smoke-test.sh` |
| 4:40-5:00 | 学习沉淀和提交 | 更新学习手册、部署说明、工作日志 | `scripts/quality-check.sh`、Git commit/push |

## 3. 角色分工

| 角色 | 本轮关注 |
| --- | --- |
| 资深架构师 | 首次域名上线是否减少变量，internal/external 短链边界是否清楚 |
| Java 后端审查者 | 短链热路径、异步事件、后台 token、事务和失败降级 |
| 前端体验审查者 | 真实域名下分享链接、结果页、后台入口是否自然 |
| SRE/运维 | DNS、安全组、HTTPS、env、Compose、smoke、回滚 |
| 大厂面试官 | 能否诚实讲清“已上线真实域名”与“未完成生产压测”的边界 |
| 学习教练 | 把域名、DNS、Nginx、TLS、APP_BASE_URL、smoke 的关系讲成可复习材料 |

## 4. 当前阻塞信息

真实域名绑定无法只靠仓库完成。完整可填写模板见 `docs/domain-launch-info-template.md`，最少需要用户提供：

```text
主域名：
服务器公网 IP：
服务器 SSH：
DNS 服务商/控制台：
域名是否已备案或接入备案：
是否已有 HTTPS 证书：
是否需要短链子域名：
```

当前停止点：前置准备完成后，先停下来等待以上信息，不继续改业务逻辑、不猜测域名、不把临时 HTTP 当成正式上线。

如果暂时没有 HTTPS 证书，本轮可以先做到：

```text
http://<主域名> 可访问
APP_BASE_URL=http://<主域名>
ALLOW_HTTP=true DOMAIN=<主域名> BASE_URL=http://<主域名> scripts/domain-bind-preflight.sh
```

但这个状态只适合临时验收，不适合长期公开宣传。

## 5. 首发推荐配置

```text
APP_BASE_URL=https://<主域名>
SHORT_LINK_MODE=internal
NGINX_HTTP_PORT=127.0.0.1:8088
VISIT_EVENT_ASYNC_QUEUE_CAPACITY=2048
VISIT_EVENT_ASYNC_DRAIN_LIMIT=64
SHORT_LINK_LAST_VISIT_TOUCH_INTERVAL_SECONDS=30
```

说明：

- `APP_BASE_URL` 决定新生成结果的分享链接域名。
- `SHORT_LINK_MODE=internal` 保持 `/s/{code}` 仍由五行后端处理。
- `NGINX_HTTP_PORT=127.0.0.1:8088` 适合宿主机 Nginx 或云证书入口转发到容器 Nginx，避免容器直接暴露公网。
- 宿主机 Nginx 模板见 `deploy/host-nginx-domain-tls.example.conf`，完整服务器步骤见 `docs/domain-server-runbook.md`。

## 6. 验收命令模板

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config

DOMAIN=<主域名> \
EXPECTED_IP=<服务器公网 IP> \
BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
scripts/domain-bind-preflight.sh

BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
scripts/production-smoke-test.sh

BASE_URL=https://<主域名> \
ADMIN_TOKEN=<admin-token> \
SHORTLINK_HITS=30 \
MAX_ASYNC_QUEUE_SIZE=0 \
MAX_ASYNC_DROPPED_EVENTS=0 \
MAX_ASYNC_BATCH_FAILURES=0 \
scripts/performance-smoke-test.sh
```

## 7. 完工后学习文档要补的内容

- 域名解析为什么要先看 A 记录和公网 IP。
- `APP_BASE_URL` 为什么会影响分享链接。
- 容器 Nginx、宿主机 Nginx、云证书/CDN 三者怎么分工。
- HTTPS 和 HSTS 为什么不是“锦上添花”，而是公开访问的基本面。
- `production-smoke-test.sh` 和 `performance-smoke-test.sh` 分别证明什么。
- 为什么首次上线先 internal，external 短链子域名要第二阶段做。
