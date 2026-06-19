# 生产压测观测清单 v1

记录日期：2026-06-14

## 使用边界

这份清单只用于“备案、授权、访问窗口、回滚方案”都确认后的公网压测。没有这些前置条件时，只能跑本地或内网小流量验证。

压测脚本必须显式设置：

```bash
ALLOW_PUBLIC_LOAD_TEST=1
DEPLOYMENT_PROFILE=public-compose
SYNTHETIC_CHANNEL=perf-test
SYNTHETIC_CAMPAIGN=production-load-test
RUN_ID=prod-YYYYMMDD-brief-name
STAGE_COOLDOWN_SECONDS=30
```

如果公网环境启用了 `VISIT_EVENT_ASYNC_MODE=rocketmq`，脚本会先读取访问事件 runtime。只要 MQ 不可用或 consumer 落库未就绪，默认拒绝压测；只有本轮目标就是验证 shadow/fallback 时，才显式设置 `ALLOW_ROCKETMQ_SHADOW_LOAD_TEST=1`。

公网模式还会 fail closed：

- `DEPLOYMENT_PROFILE` 不能保留 `local-h2` / `local` / `dev`。
- 访问事件 runtime 如果不可观测或缺少关键字段，脚本拒绝继续。
- 压测前 `droppedAsyncEvents`、`batchWriteFailures` 基线必须不超过阈值；每阶停止条件再看本阶增量。
- 公网多阶压测要求 `STAGE_COOLDOWN_SECONDS>=30`，脚本会 fail closed，避免未冷却就进入下一阶。

如果 fail closed 在前置阶段触发，脚本会写出 `preflight-failed.json`。复盘时先看这个文件，而不是把“没有报告”误认为脚本没执行。

## 压测前检查

| 检查项 | 目标 | 记录方式 |
| --- | --- | --- |
| 授权窗口 | 明确开始/结束时间、最大并发、执行人 | 写入压测报告备注 |
| 备案/域名 | 域名可公网访问，不被备案拦截 | 浏览器 + `curl -I` |
| 回滚动作 | 明确如何暂停压测、降并发、重启服务 | 写入 runbook |
| 数据口径 | 压测流量必须带 `channel=perf-test` | 后台默认视图排除 |
| 部署画像 | 明确 `DEPLOYMENT_PROFILE` 是 `local-h2`、`compose-mysql` 还是 `public-compose` | 写入报告环境卡片 |
| 阶梯冷却 | 公网多阶压测至少冷却 30 秒 | `STAGE_COOLDOWN_SECONDS` 写入报告环境卡片 |
| 就绪检查 | `/api/readiness` 返回 `UP`，核心业务表可查询 | 写入报告环境卡片或 smoke 输出 |
| RocketMQ 状态 | 如果启用 MQ，确认 producer、consumer、fallback 目标 | runtime 截图或 `/api/admin/visit-events/runtime` |
| 服务基线 | 压测前 5 分钟 CPU、内存、GC、DB、Redis 正常 | 每项截图或命令输出 |

## 每阶采集表

每一个并发阶梯都要采集“压测前、压测中、压测后冷却完成”三份状态。不要只保存压测脚本的 summary。

| 观测面 | 采什么 | 为什么重要 |
| --- | --- | --- |
| 应用脚本 | P50/P95/P99、错误率、分接口 P95、runtime before/after | 判断用户体验和应用内队列是否健康 |
| 容器资源 | CPU%、内存、网络、块 IO | 判断是否打满单机资源 |
| JVM | GC 次数/耗时、堆使用、线程数 | 判断是否 GC 或线程调度导致延迟 |
| Nginx | status、upstream response time、429/499/5xx | 判断入口层、限流和上游延迟 |
| MySQL | processlist、慢 SQL、连接数、锁等待 | 判断数据库是否成为瓶颈 |
| Redis | used_memory、connected_clients、keyspace_hits/misses | 判断缓存命中和连接状态 |
| 业务 readiness/runtime | readinessStatus、queueSize、droppedAsyncEvents、batchWriteFailures、healthStatus | 判断核心表是否可用，以及统计链路是否丢数 |

## 建议命令模板

以下命令是记录模板，执行时按实际服务器路径、容器名和权限调整。

```bash
# 容器资源
docker stats --no-stream

# Nginx 最近状态码和 upstream time，需要按实际日志格式调整
tail -n 200 /var/log/nginx/access.log

# 后端最近错误和 GC 关键词，需要按实际日志路径调整
tail -n 300 /opt/wuxing-persona-card/logs/backend.log

# MySQL 连接和慢查询线索
docker exec -it wuxing-mysql mysql -uroot -p -e "SHOW FULL PROCESSLIST;"

# Redis 命中和内存
docker exec -it wuxing-redis redis-cli INFO stats
docker exec -it wuxing-redis redis-cli INFO memory
```

## 停止条件

任意条件触发就停止当前阶梯，不继续冲更高并发：

| 条件 | 为什么停 |
| --- | --- |
| P95 超过目标阈值 | 用户已经感知明显等待 |
| 错误率超过 1% | 可用性开始受损 |
| `droppedAsyncEvents` 基线或本阶增量超过阈值 | 统计链路已经丢数 |
| `batchWriteFailures` 基线或本阶增量超过阈值 | 访问事件落库不稳定 |
| `healthStatus=danger` | 后端 runtime 已给出危险态 |
| 队列水位超过阈值 | 异步削峰接近失效 |
| Nginx 出现持续 5xx/429 | 入口或上游保护已触发 |
| MySQL 出现明显锁等待/慢 SQL 堆积 | 继续压会污染结论并伤害服务 |

## 阶梯节奏

公网第一轮建议：

```text
health:    1,2,4,8,16,32
shortlink: 1,2,4,8,16,32
result:    1,2,4,8,16,32
admin:     1,2,4,8,16
mixed:     1,2,4,8,16,32
```

每阶要求：

- `REQUESTS_PER_STAGE >= concurrency * 2`。
- 脚本公网模式会强制要求 `REQUESTS_PER_STAGE >= max(STEPS) * 2`，避免样本数过低时把配置阶梯误讲成稳态容量。
- 脚本公网模式会强制要求 `STAGE_COOLDOWN_SECONDS>=30`，让队列、连接池、JVM 和数据库状态有冷却窗口。
- 每阶结束后等待队列回落到安全水位。
- 每阶之间至少冷却 30-60 秒。
- `mixed` 永远放最后，不能一开始就用混合流量冲顶。

## 公网分路径压测命令模板

以下命令只能在备案、公网访问、授权窗口、停止条件和服务端观测都确认后执行。`BASE_URL`、`OUT_DIR`、阈值和最大并发要按当时服务器规格调整；第一轮建议保守使用 `1,2,4,8,16,32`。

```bash
# 1. health: 先确认入口和后端整体可用
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=replace-with-production-admin-token \
ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=public-compose \
SYNTHETIC_CHANNEL=perf-test SYNTHETIC_CAMPAIGN=production-load-test \
RUN_ID=prod-$(date +%Y%m%d%H%M%S)-health \
WORKLOAD=health STEPS=1,2,4,8,16,32 REQUESTS_PER_STAGE=64 \
STAGE_COOLDOWN_SECONDS=30 STOP_P95_MS=300 STOP_ERROR_RATE=0.01 \
OUT_DIR=docs/performance-reports/prod-health-$(date +%Y%m%d%H%M%S) \
scripts/performance-limit-test.sh

# 2. shortlink: 单压分享跳转热路径
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=replace-with-production-admin-token \
ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=public-compose \
SYNTHETIC_CHANNEL=perf-test SYNTHETIC_CAMPAIGN=production-load-test \
RUN_ID=prod-$(date +%Y%m%d%H%M%S)-shortlink \
WORKLOAD=shortlink STEPS=1,2,4,8,16,32 REQUESTS_PER_STAGE=64 \
STAGE_COOLDOWN_SECONDS=30 STOP_P95_MS=800 STOP_ERROR_RATE=0.01 \
OUT_DIR=docs/performance-reports/prod-shortlink-$(date +%Y%m%d%H%M%S) \
scripts/performance-limit-test.sh

# 3. result: 单压结果读取
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=replace-with-production-admin-token \
ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=public-compose \
SYNTHETIC_CHANNEL=perf-test SYNTHETIC_CAMPAIGN=production-load-test \
RUN_ID=prod-$(date +%Y%m%d%H%M%S)-result \
WORKLOAD=result STEPS=1,2,4,8,16,32 REQUESTS_PER_STAGE=64 \
STAGE_COOLDOWN_SECONDS=30 STOP_P95_MS=800 STOP_ERROR_RATE=0.01 \
OUT_DIR=docs/performance-reports/prod-result-$(date +%Y%m%d%H%M%S) \
scripts/performance-limit-test.sh

# 4. admin: 单压后台 overview，先不要超过 16 阶
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=replace-with-production-admin-token \
ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=public-compose \
SYNTHETIC_CHANNEL=perf-test SYNTHETIC_CAMPAIGN=production-load-test \
RUN_ID=prod-$(date +%Y%m%d%H%M%S)-admin \
WORKLOAD=admin STEPS=1,2,4,8,16 REQUESTS_PER_STAGE=32 \
STAGE_COOLDOWN_SECONDS=30 STOP_P95_MS=1200 STOP_ERROR_RATE=0.01 \
OUT_DIR=docs/performance-reports/prod-admin-$(date +%Y%m%d%H%M%S) \
scripts/performance-limit-test.sh

# 5. mixed: 最后再跑混合链路
BASE_URL=https://wuxingcard.cn ADMIN_TOKEN=replace-with-production-admin-token \
ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=public-compose \
SYNTHETIC_CHANNEL=perf-test SYNTHETIC_CAMPAIGN=production-load-test \
RUN_ID=prod-$(date +%Y%m%d%H%M%S)-mixed \
WORKLOAD=mixed STEPS=1,2,4,8,16,32 REQUESTS_PER_STAGE=64 \
STAGE_COOLDOWN_SECONDS=30 STOP_P95_MS=800 STOP_ERROR_RATE=0.01 \
OUT_DIR=docs/performance-reports/prod-mixed-$(date +%Y%m%d%H%M%S) \
scripts/performance-limit-test.sh
```

## 报告补充字段

压测脚本会生成 `summary.json`、`report.md` 和 CSV。生产复盘时还要人工补充：

| 字段 | 示例 |
| --- | --- |
| 云服务器规格 | 2C2G / 4C8G |
| 部署模式 | Nginx + Spring Boot + MySQL + Redis |
| 压测来源 | 本机 / 云压测机 / 第三方工具 |
| 授权窗口 | 2026-xx-xx 22:00-22:30 |
| 最大并发上限 | 32 |
| 停止原因 | P95 超阈值 / runtime danger / 人工停止 |
| 脚本自动结论 | 最慢接口、runtime 风险、本地/公网外推边界 |
| 主要瓶颈假设 | Nginx、Tomcat、DB、Redis、JVM、网络、应用逻辑 |
| 下一步动作 | 只改一个变量，然后复测 |

## 面试表达

> 生产压测我不会只拿一个 P95 数字说结论。我会把每阶请求指标、应用 runtime、容器资源、Nginx、MySQL、Redis 和 JVM 状态放在同一张记录里看。如果 HTTP 还没报错但 `droppedAsyncEvents` 增加，我会先停，因为统计链路已经不可信；如果 health 也变慢，就不急着改业务 SQL，而是先看整体资源、线程调度和入口层。
