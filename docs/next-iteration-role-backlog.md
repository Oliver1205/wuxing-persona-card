# 下一轮多角色优化 Backlog

记录日期：2026-06-11

本文档记录角色 agent 审视后的下一轮候选工作。它不是承诺全部立即实现，而是帮助主线程在后续八小时工作流中按影响面排序。

## 1. 产品经理 + 普通访问者审视结果

| 优先级 | 问题 | 用户场景 | 相关位置 | 建议动作 |
| --- | --- | --- | --- | --- |
| P0 | 首访承诺和进入后的第一步不一致 | 首页说 90 秒完成 5 道题，进入测试后先看到出生信息，用户可能觉得承诺落差 | `frontend/src/pages/GuidePage.vue`、`frontend/src/pages/TestPage.vue` | 首页文案改为“出生年月 + 5 道题”，或在 CTA 附近说明日期/时段可跳过 |
| P0 | 基础信息页信息密度过高 | 移动端用户先看到年份滑杆、手输年份、月份、日期、时段，容易误以为都必须认真填写 | `frontend/src/pages/TestPage.vue` | 下一轮优先突出年份和月份，日期/时段降为次级可选项 |
| P1 | 答题自动前进削弱掌控感 | 用户误触或想确认选项时，650ms 后已经进入下一题 | `frontend/src/pages/TestPage.vue` | 保留选中反馈，评估改为手动下一题或延长自动前进时间 |
| P1 | 结果页缺少“为什么像我”的快速解释 | 用户看到星官和比例后，还需要更具体的生活化判断才愿意分享 | `frontend/src/pages/ResultPage.vue`、`ResultTextService.java` | 前置“最像你的 3 个表现”，再展示五行比例解释 |
| P1 | 后台运营页工程字段抢占注意力 | 运营者想看转化和分享效果，却被事件枚举、hash、runtime 信息分散 | `frontend/src/pages/AdminDashboard.vue`、`frontend/src/pages/AdminShortLinkDetail.vue` | 默认展示人话漏斗，hash/runtime/原始事件收进明细或调试区 |

## 2. 主线程初步取舍

下一轮最适合先做两个小切口：

1. 首页承诺文案和测试页出生信息说明统一。这是最低风险的 P0，可以快速修复用户预期落差。已在 Phase 21 落地。
2. 测试页基础信息分层。先不改后端 payload，只把日期/时段视觉上降级为可选补充，避免扩大业务范围。已在 Phase 21 落地。

暂缓项：

- 结果文案重排需要同时考虑分享图和后端文案模板，适合单独一轮。
- 后台运营信息分层会影响多个表格和筛选区，适合等后端性能审视一起决定。
- 自动前进策略需要浏览器验证真实手感，不能只凭静态判断。

## 3. 资深架构师 + 后端性能审视结果

| 优先级 | 问题 | 峰值风险 | 相关位置 | 验证方式 |
| --- | --- | --- | --- | --- |
| P0 | 访问事件仍在请求线程同步写库 | 微信群或朋友圈集中打开短链时，`/s/{shortCode}` 每次 302 都同步 `INSERT visit_event`，数据库抖动会拖慢跳转 | `InternalShortLinkProvider`、`VisitEventService` | 扩展 `scripts/performance-smoke-test.sh`，将 `SHORTLINK_HITS` 提到 500/1000，对比短链平均耗时和 TP95，并检查事件落库/丢弃计数 |
| P0 | 后台短链列表仍可能实时扫访问明细 | 事件表增长后，后台 `COUNT(DISTINCT ...) GROUP BY short_code` 会和线上写入抢 IO/CPU | `AdminStatService`、`VisitEventMapper`、`ShortLinkDailyMetricMapper` | 造多日访问数据，先执行聚合，再对比 `/api/admin/short-links` SQL 耗时和结果一致性 |
| P1 | external 模式统计缺少短缓存 | 一页短链可能触发多次外部 HTTP 统计请求，外部慢会拖慢后台 | `ExternalShortLinkStatsAdapter`、`RestExternalShortLinkClient` | mock 外部服务加 500ms 延迟，连续访问后台列表，第二次应命中缓存 |
| P1 | 热短码计数还没有 Redis 增量缓冲 | 如果未来恢复短链表实时计数，热点行会被高频更新 | `ShortLinkMapper`、`InternalShortLinkProvider` | 同一短码高并发访问时，MySQL `UPDATE short_link` 次数应接近周期级而不是请求级 |
| P2 | 迁移治理和 EXPLAIN 基线不足 | 重复 DDL 靠 `continue-on-error` 吞错，真实索引漂移不易发现 | `schema.sql`、`application.yml` | MySQL 容器执行 `SHOW INDEX` 和核心 SQL `EXPLAIN`，确认索引命中 |

## 4. 后端主线程初步取舍

下一轮最适合优先做两个小切口：

1. 访问事件异步化。先做单机 `BlockingQueue` + 后台批量 flush，队列满时丢弃低价值事件并记录计数，让短链跳转只负责入队。
2. 后台短链列表历史日期读 `short_link_daily_metric`，今天继续读 live event，把大部分历史运营查询从明细表扫描移到小表聚合。

暂缓项：

- Redis 热计数需要先明确 PV/UV/UIP 的最终一致性口径，适合在日聚合链路稳定后做。
- external 统计缓存适合和 external mock 测试一起做，避免只做缓存没有可验证证据。
- Flyway/Liquibase 迁移治理价值很高，但会影响启动和部署方式，适合作为独立生产化任务。

## 5. 待补充审视

- 大厂面试官压力审视：后续可转为独立追问清单。
- 美术经理审视：结合真实截图归档后再补。
