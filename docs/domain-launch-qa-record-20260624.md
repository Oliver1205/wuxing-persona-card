# 真实域名上线前 QA 记录 2026-06-24

## Scope

- 域名：`wuxingcard.cn` / `www.wuxingcard.cn`
- 触发背景：腾讯云备案订单已通过管局审核，正式备案号待通信管理局短信/邮箱下发。
- 本轮目标：把备案展示、正式域名配置、上线文档和可复跑预检接入上线前闭环。

## Key Changes

- 新增全局 `RegulatoryFooter`，用于展示娱乐性声明和正式备案号。
- 前端备案号通过 `VITE_ICP_RECORD_NO` 注入，备案链接通过 `VITE_ICP_LINK` 注入，默认指向 `https://beian.miit.gov.cn/`。
- Docker 前端构建已接收 `VITE_ICP_RECORD_NO` / `VITE_ICP_LINK` build args。
- `deploy-preflight.sh` 已在正式 `wuxingcard.cn` / `www.wuxingcard.cn` 域名下强制要求备案号配置。
- `set-production-entry.sh` 支持通过 `ICP_RECORD_NO` 写入服务器本地 `deploy/.env`。

## Must Not Do

- 不要把腾讯云备案订单号、账号 ID 或审核通知编号展示在网站底部。
- 不要在正式备案号下发前声称备案号展示已经完成。
- 不要跳过前端重建；Vite 的 `VITE_*` 变量是构建期注入。

## Commands To Run After ICP Number Arrives

```bash
cd /opt/wuxing-persona-card
ICP_RECORD_NO='<通信管理局下发的正式备案号>' \
BASE_URL=https://wuxingcard.cn \
NGINX_HTTP_PORT_VALUE=127.0.0.1:8088 \
APPLY_COMPOSE=true \
scripts/set-production-entry.sh

DOMAIN=wuxingcard.cn EXPECTED_IP=82.157.137.36 scripts/domain-dns-readiness.sh
DOMAIN=wuxingcard.cn BASE_URL=https://wuxingcard.cn scripts/domain-bind-preflight.sh

set -a
. deploy/.env
set +a
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN="$ADMIN_TOKEN" scripts/production-smoke-test.sh
```

## Verification Run In This Local Pass

| Check | Result | Notes |
| --- | --- | --- |
| `npm --prefix frontend run build` | Pass | 默认构建通过，未保留测试备案号。 |
| `VITE_ICP_RECORD_NO='<测试备案号>' npm --prefix frontend run build` | Pass | 仅用于本地验证备案 footer 构建期注入，未保留测试值。 |
| `node scripts/verify-frontend-contracts.mjs` | Pass | 覆盖备案变量、MIIT 链接、分享/短链/移动视觉合同。 |
| `git diff --check` | Pass | 未发现空白错误。 |
| `bash -n scripts/deploy-preflight.sh && bash -n scripts/set-production-entry.sh` | Pass | 新增 ICP 逻辑语法通过。 |
| `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` | Pass | internal 模式配置可展开，nginx build args 包含 ICP 变量。 |
| `docker compose --env-file deploy/.env.external.example -f deploy/docker-compose.yml -f deploy/docker-compose.external-mode.yml config` | Pass | external 模式配置可展开，nginx build args 包含 ICP 变量。 |
| Playwright mobile preview, `390 x 844` | Pass | footer 可见，链接为 `https://beian.miit.gov.cn/`，`scrollWidth=390`，控制台无 warning/error。截图：`/private/tmp/wuxing-icp-footer-home-mobile.png`。 |
| `scripts/quality-check.sh` | Blocked by environment | Maven、前端构建、合同、预览检查已通过；执行到 `scripts/mysql-schema-smoke-test.sh` 时 Docker API `/info` 返回 500。提权重跑 schema smoke 仍为同一 Docker daemon 错误。 |

## Residual Risk

- 正式备案号仍未在仓库中填写；必须等通信管理局下发后在服务器本地配置。
- 本地 Docker daemon 当前不可用，fresh MySQL schema smoke 需要在 Docker Desktop 恢复后补跑。
- 真实公网压测仍需要授权窗口、停止条件、服务端观测和冷却间隔，不应和 production smoke 混为一个结论。
