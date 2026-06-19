# 本地预览与短链代理 Runbook

## 目标

本地预览不是只看页面能不能打开，还要验证五件事：

1. 后端生成的 `shortUrl` 是否指向当前前端地址。
2. 前端 `/s/{shortCode}` 是否正确代理到当前后端。
3. 双人匹配候选、创建和结果回读是否能走通。
4. 后台 `/admin` 和访问事件 runtime 是否可用。
5. 后台短链 CSV 导出是否能按当前筛选口径拿到数据。

这个 runbook 用来避免“前端页面能打开，但短链代理、匹配接口或后台导出打到旧后端/错误端口”的调试误判。

## 推荐端口

| 服务 | 地址 |
| --- | --- |
| 前端 Vite | `http://127.0.0.1:5175` |
| 后端 Spring Boot local profile | `http://127.0.0.1:48081` |
| 后台 token | `dev-token` |

## 启动命令

后端：

```bash
cd backend
APP_BASE_URL=http://127.0.0.1:5175 \
mvn spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--server.port=48081
```

前端：

```bash
cd frontend
BACKEND_PROXY_TARGET=http://127.0.0.1:48081 npm run dev -- --port 5175
```

## 一键验证

`scripts/quality-check.sh` 会覆盖脚本语法、后端单测、前端构建、前端契约扫描、静态预览校验，并启动 Chromium 验证 `file://` 静态预览流程。它不会启动本地前后端端口，也不会真正访问 `result -> /s -> match -> admin CSV` 链路。端口可用后，必须单独运行下面的 live smoke 才能宣称本地前后端联调通过。

如果要一次性验证真实前端、后端、移动端 E2E、showcase 截图和交付物几何，可以在端口都已启动后运行 live gate：

```bash
FRONTEND_URL=http://127.0.0.1:5175 \
BACKEND_URL=http://127.0.0.1:48081 \
scripts/frontend-live-gate.sh
```

```bash
FRONTEND_URL=http://127.0.0.1:5175 \
BACKEND_URL=http://127.0.0.1:48081 \
ADMIN_TOKEN=dev-token \
scripts/local-preview-smoke-test.sh
```

脚本会创建一个本地测试结果，并默认写入：

- `X-Channel: perf-test`
- `X-Campaign: local-preview-smoke`

因此数据中台默认运营口径会排除这类验证流量。

## 脚本验证什么

| 检查 | 失败时优先看 |
| --- | --- |
| `GET BACKEND_URL/api/readiness` 返回 `UP` | H2/MySQL schema 是否初始化，核心表是否可查询 |
| `GET FRONTEND_URL/api/readiness` 返回 `UP` | 前端 dev server 或 Nginx 的 `/api` 同源代理是否指向当前后端 |
| `shortUrl == FRONTEND_URL/s/{shortCode}` | 后端 `APP_BASE_URL` 是否与当前前端地址一致 |
| `GET /api/matches/candidates/{shortCode}` 返回同一短码 | 双人匹配候选接口、短链绑定、测试结果是否存在 |
| `POST /api/matches` 返回 `matchId/currentShortCode/compatibilityScore` | 匹配提交、答案结构、结果生成和短链生成是否联通 |
| `GET /api/matches/{partner}/{current}` 回读同一 `matchId` | 匹配结果持久化、路由参数顺序和结果查询是否一致 |
| `GET FRONTEND_URL/s/{shortCode}` 返回 `301/302` | 前端 `BACKEND_PROXY_TARGET` 是否指向当前后端 |
| `Location` 包含 `/result/{resultId}?sc={shortCode}` | 短链绑定、代理或结果路由是否错配 |
| `GET FRONTEND_URL/s/{shortCode}?channel=perf-test&campaign=result-card` 继续透传 query | 分享链接 query 透传是否可用，同时避免 smoke 污染默认运营口径 |
| external 短链 smoke 的 `Location` 包含 `channel=share&campaign=result-card` | 外部短链创建出的结果落地页是否直接进入分享回流态 |
| `/admin` 返回 SPA shell | 前端路由 fallback 是否可用 |
| runtime 非 `danger` | 访问事件队列、批量写入、RocketMQ fallback 状态 |
| `GET /api/admin/short-links/export?keyword={shortCode}&includeSynthetic=true` 返回 CSV | 后台 token、导出口径、短链列表筛选和 CSV header 是否一致 |
| `GET FRONTEND_URL/api/admin/short-links/export?keyword={shortCode}&includeSynthetic=true` 返回同口径 CSV | 前端同源 `/api` 代理是否覆盖后台导出接口 |
| `GET /api/admin/short-links/export?keyword={shortCode}` 不包含当前短码 | 默认运营口径是否继续排除 `perf-test` 验证流量 |

CSV 导出检查会断言 `Content-Type: text/csv`、`Content-Disposition` 包含 `wuxing-short-links`，确认文件有 UTF-8 BOM，并确认完整表头等于 `shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt`。真实前端分享链接仍由 `withShareAttribution` 生成 `channel=share&campaign=result-card`，这条由前端契约脚本静态锁定；本地 smoke 只用 `perf-test` 口径做 query 透传验证。

注意：不要用 `HEAD /s/{shortCode}` 判断短链是否可用；当前用户访问路径是 `GET`，脚本也按 `GET` 验证。

## 手机局域网预览

如果手机要访问电脑局域网 IP，例如 `http://192.168.1.3:5175`，后端也要同步使用同一个前端地址：

```bash
APP_BASE_URL=http://192.168.1.3:5175 \
mvn spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--server.port=48081
```

前端仍然代理到电脑本机后端：

```bash
BACKEND_PROXY_TARGET=http://127.0.0.1:48081 npm run dev -- --host 0.0.0.0 --port 5175
```

然后用同一个脚本验证：

```bash
FRONTEND_URL=http://192.168.1.3:5175 \
BACKEND_URL=http://127.0.0.1:48081 \
ADMIN_TOKEN=dev-token \
scripts/local-preview-smoke-test.sh
```

## 公网安全边界

`scripts/local-preview-smoke-test.sh` 默认拒绝公网 URL，因为它会创建结果和访问短链。只有在明确授权的测试窗口内，才允许设置：

```bash
ALLOW_PUBLIC_PREVIEW_SMOKE=1
```

即使设置了 `ALLOW_PUBLIC_PREVIEW_SMOKE=1`，脚本也不是“零生产写入”：它会创建 `result`、`short_link` 和 `visit_event` 相关验证数据。公共环境只适合在 staging 或明确授权的测试窗口运行，跑完后用 `channel=perf-test` 作为 synthetic 数据审计和清理口径。

公网压测和生产验证仍应优先使用：

- [`production-load-observability-checklist.md`](production-load-observability-checklist.md)
- [`performance-reports/README.md`](performance-reports/README.md)
- [`performance-optimization-plan.md`](performance-optimization-plan.md)
