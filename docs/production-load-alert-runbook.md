# 生产压测与告警演练 Runbook

记录日期：2026-06-12

本文档用于把“后端能扛瞬时流量、低延迟”从口头描述推进到可验证演练。当前仓库已经有短链热路径优化、性能 smoke、异步事件 runtime 和 Nginx 限流配置；但在真实服务器、正式域名、固定数据量和可观测指标下完成压测前，不能宣称已验证生产 QPS。

## 1. 演练目标

| 目标 | 要证明什么 | 不能夸大的部分 |
| --- | --- | --- |
| 短链热路径低延迟 | `/s/{shortCode}` 在连续访问下保持可接受 avg / P95，并返回正确 302 | 不等于多机房短链平台容量 |
| 统计写入不阻塞跳转 | `asyncQueueSize` 不持续增长，`asyncDroppedEvents` 不异常上升，worker 存活 | 队列无积压不代表事件 100% 不丢 |
| 后台查询不拖慢核心链路 | `/api/admin/overview` 在缓存和聚合策略下稳定返回 | 后台强实时统计仍可能昂贵 |
| 告警能被触发和恢复 | 健康检查、错误率、延迟、队列积压、磁盘/内存有记录 | 没有告警平台前只能做脚本化演练 |

## 2. 环境前提

演练前确认：

- 已在真实服务器或稳定测试机部署 Docker Compose。
- `APP_BASE_URL`、`ADMIN_TOKEN`、`HASH_SALT`、数据库密码已经替换默认值。
- Nginx `/api/**`、`/api/events`、`/s/**` 限流配置已加载。
- MySQL、Redis、backend、nginx 容器均健康。
- 可以查看服务器 CPU、内存、磁盘、容器日志和数据库连接数。

基础检查：

```bash
scripts/deploy-preflight.sh deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
BASE_URL=https://your-domain.example \
ADMIN_TOKEN=<your-admin-token> \
scripts/production-smoke-test.sh
```

## 3. 分层压测顺序

不要一上来做全站混合压测。先分层看清瓶颈，再做组合。

| 顺序 | 链路 | 目的 | 推荐入口 |
| --- | --- | --- | --- |
| 1 | 健康检查 | 确认环境稳定 | `/api/health` |
| 2 | 创建结果 | 确认写链路和短链生成可用 | `POST /api/results` |
| 3 | 短链跳转 | 验证传播峰值入口 | `/s/{shortCode}` |
| 4 | 后台总览 | 验证统计查询和缓存 | `/api/admin/overview` |
| 5 | 组合流量 | 模拟用户分享后的混合访问 | 创建结果 + 短链跳转 + 后台刷新 |

## 4. 性能 Smoke 基线

先用仓库脚本建立轻量基线：

```bash
BASE_URL=https://your-domain.example \
ADMIN_TOKEN=<your-admin-token> \
SHORTLINK_HITS=100 \
ADMIN_HITS=5 \
MAX_SHORTLINK_AVG_MS=120 \
MAX_SHORTLINK_P95_MS=220 \
MAX_ADMIN_AVG_MS=250 \
MAX_ADMIN_P95_MS=500 \
MAX_ASYNC_QUEUE_SIZE=0 \
MAX_ASYNC_DROPPED_EVENTS=0 \
MAX_ASYNC_BATCH_FAILURES=0 \
scripts/performance-smoke-test.sh
```

记录输出：

```text
shortlinkAvgMs=
shortlinkP95Ms=
adminAvgMs=
adminP95Ms=
asyncQueueSize=
asyncQueueCapacity=
asyncDroppedEvents=
asyncTotalFlushedEvents=
asyncLastFlushAt=
asyncLastBatchSize=
asyncBatchWriteFailures=
asyncWorkerAlive=
maxAsyncQueueSize=
maxAsyncDroppedEvents=
maxAsyncBatchFailures=
```

判断口径：

- `shortlinkAvgMs` 和 `shortlinkP95Ms` 用来观察短链热路径是否退化。
- `asyncQueueSize` 用来看低延迟是否靠堆积事件换来。
- `asyncTotalFlushedEvents` 和 `asyncLastFlushAt` 用来看后台 worker 是否持续排水。
- `asyncLastBatchSize` 用来看当前批量写入是否真的在合并事件。
- `asyncDroppedEvents` 如果明显上升，要检查队列容量、批量写库速度和数据库连接池。
- `asyncBatchWriteFailures` 如果上升，要检查数据库写入、表锁、连接池和批量 SQL。
- `asyncWorkerAlive=false` 是严重故障，必须停止压测并看后端日志。
- `MAX_ASYNC_QUEUE_SIZE`、`MAX_ASYNC_DROPPED_EVENTS` 和 `MAX_ASYNC_BATCH_FAILURES` 默认留空只观察；设为 `0` 时，smoke 会把“无积压、无丢弃、无批量写失败”变成硬性门禁。
- 短链列表里的 `metricSource` 用于区分统计口径：`live_event` 表示实时事件聚合，`daily_metric` 表示日聚合表，`external` 表示独立短链服务统计。压测复盘时不要把三种口径混在一起比较。

压测调参入口：

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `VISIT_EVENT_ASYNC_QUEUE_CAPACITY` | `2048` | 调整访问事件异步队列容量，适合在短链峰值演练时观察积压和丢弃边界。 |
| `VISIT_EVENT_ASYNC_DRAIN_LIMIT` | `64` | 调整后台 worker 单次批量写库上限，适合在数据库写入压力和延迟之间取平衡。 |
| `SHORT_LINK_LAST_VISIT_TOUCH_INTERVAL_SECONDS` | `30` | 调整热门短码 `last_visit_at` 的低频更新间隔，避免传播峰值下反复打热同一行。 |

## 5. 正式压测记录模板

每次压测必须记录固定信息，否则不要把结果写进简历或宣传页。

| 字段 | 示例 |
| --- | --- |
| 日期时间 | 2026-06-12 22:00-22:20 |
| 机器规格 | 2C4G / 4C8G / 具体云厂商 |
| 部署版本 | Git SHA |
| 数据规模 | `user_result` 行数、`short_link` 行数、`visit_event` 行数 |
| 压测工具 | performance smoke / wrk / k6 / Gatling |
| 并发模型 | 并发数、持续时间、预热时间 |
| 测试入口 | `/s/{shortCode}`、`/api/admin/overview` |
| 指标 | QPS、avg、P95、P99、错误率 |
| 系统指标 | CPU、内存、磁盘、网络、DB 连接 |
| 业务指标 | asyncQueueSize、asyncTotalFlushedEvents、asyncDroppedEvents、asyncBatchWriteFailures、workerAlive |
| 结论 | 可接受 / 需优化 / 环境异常 |

## 6. 告警演练清单

如果没有正式告警平台，先用脚本、日志和人工检查完成演练记录；后续再接 Prometheus、Grafana、云监控或日志平台。

| 告警项 | 触发方式 | 期望现象 | 恢复动作 |
| --- | --- | --- | --- |
| 后端不可用 | 停止 backend 容器 | `/api/health` 失败，production smoke 失败 | 重启 backend，确认健康检查恢复 |
| Redis 不可用 | 停止 Redis 容器 | 缓存 warn 增加，主链路应回源 DB | 恢复 Redis，观察 warn 下降 |
| 事件队列积压 | 提高短链访问量或降低数据库写入能力 | `asyncQueueSize` 上升 | 降低流量，检查 DB 写入和队列容量 |
| 事件丢弃 | 队列满或 worker 异常 | `asyncDroppedEvents` 上升 | 停止压测，排查 worker、DB、队列容量 |
| 批量写失败 | 模拟数据库写入异常或连接池耗尽 | `asyncBatchWriteFailures` 上升 | 停止压测，查看 DB 锁等待、连接池和批量 SQL |
| 后台查询变慢 | 增加后台刷新或扩大日期范围 | `adminP95Ms` 上升 | 检查 overview cache、索引、聚合表 |
| 磁盘空间不足 | 模拟日志/备份占用 | 容器写入失败风险 | 清理日志，验证备份和恢复策略 |

## 7. 压测后复盘

压测完成后必须回答：

1. 哪个接口最先变慢？
2. 是 CPU、DB、Redis、网络、连接池还是应用线程成为瓶颈？
3. `asyncQueueSize` 是否回落？
4. `asyncDroppedEvents` 是否增加？
5. `asyncTotalFlushedEvents` 是否增长，`asyncLastFlushAt` 是否刷新？
6. `asyncBatchWriteFailures` 是否增加？
7. 后台 overview 的缓存命中是否有效？
8. Nginx 限流是否保护了 backend？
9. 是否出现 5xx、连接超时或重定向错误？
10. 是否需要调大队列、优化 SQL、增加索引、改限流或引入 MQ？

## 8. 面试表达边界

可以说：

> 当前项目已经完成单机阶段的短链热路径优化、Redis 缓存、事件异步批量写、后台 overview 短缓存、性能 smoke 和 runtime 可观测指标。下一步会用固定环境、固定数据规模和固定并发模型做正式压测，并记录 QPS、P95、错误率和队列积压。

不要说：

> 已经验证生产高并发。

除非你已经有完整压测报告、机器规格、数据规模、指标截图和复盘结论。

## 9. 下一步可落地项

- 将 `performance-smoke-test.sh` 的输出保存为时间戳日志。
- 用 `wrk` 或 `k6` 增加单接口压测脚本，先只测 `/s/{shortCode}`。
- 将 `/api/admin/visit-events/runtime` 的队列积压、丢弃数和 worker 存活检查纳入每次 smoke 输出。
- 接入真实告警平台前，先把健康检查和 runtime 检查加入部署验收清单。
