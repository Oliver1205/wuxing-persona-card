# 2026-07-04 数据中台升级 QA 记录

## Scope

本轮按“B 工程分析混合型”方向重构 `/admin` 数据中台：后台从瀑布式统计页改成左侧分页式工程工作台，第一屏聚焦当前在线用户、今日 PV、今日生成结果和系统健康，后续分页承载流量趋势、120 人格分布、采集链路与系统健康。

## Key Changes

- 前端 `AdminDashboard.vue` 改为五页结构：实时概览、流量趋势、人格分布、采集链路、系统健康。
- 后端 `AdminOverviewVO` 新增平均完成耗时、热门人格、120 人格分布字段。
- `AdminStatService` 新增 session 级平均完成耗时、人格排行和人格分布聚合。
- `RedisCacheService` 在结果生成时写入热门人格、星官和五行组合 ZSET，为后续实时榜单留接口。
- `docs/admin-data-center-guide.md`、`docs/admin-metric-dictionary.md`、`docs/api-spec.md` 同步改成工程数据中台口径。

## Verified

| Check | Result |
| --- | --- |
| `npm --prefix frontend run build` | Passed |
| `node scripts/verify-frontend-contracts.mjs` | Passed |
| `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest test` | Passed |
| `curl -s -i http://127.0.0.1:48082/api/readiness` | HTTP 200, core tables ok |
| `curl -s -i -H 'X-Admin-Token: dev-token' http://127.0.0.1:48082/api/admin/overview?forceRefresh=true` | HTTP 200, returned `averageCompletionSeconds`, `popularPersonas`, `personaDistribution` |
| `curl -s -i -H 'X-Admin-Token: dev-token' http://127.0.0.1:48082/api/admin/metrics/realtime` | HTTP 200 |
| `env E2E_BASE_URL=http://127.0.0.1:5176 E2E_ADMIN_TOKEN=dev-token node scripts/verify-admin-dashboard-pagination.mjs` | Passed, five nav pages visible, no desktop body overflow |
| `scripts/verify-eight-hour-artifacts.sh` | Passed |
| `git diff --check` | Passed |

## Screenshots

- `docs/screenshots/showcase/admin-dashboard-realtime-20260704.png`：实时概览首屏，验证在线用户、今日 PV、今日结果、系统健康和在线趋势。
- `docs/screenshots/showcase/admin-dashboard-pagination-20260704.png`：系统健康分页，验证左侧目录分页和运维操作区。

## Browser DOM Review

- Desktop viewport: `1440 x 1100`.
- Left nav count: `5`.
- Nav labels: `实时概览`、`流量趋势`、`人格分布`、`采集链路`、`系统健康`.
- Body scroll width: `1440`.
- Body client width: `1440`.
- No page-level horizontal overflow detected.

## Residual Risk

- 当前排行榜页面以 MySQL 日期聚合为权威口径，Redis ZSET 已写入但暂未作为单独“实时热榜”接口展示。
- 本地验证使用 H2 + dev-token；线上部署仍需在服务器执行 production smoke，确认 MySQL/Redis/Nginx 环境一致。
- 结果页停留时长暂未纳入本轮指标，因为需要新增前端停留事件和更明确的离开页触发边界。
