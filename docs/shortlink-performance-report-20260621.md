# 短链跳转压测报告 2026-06-21

## 一句话结论

本轮优化后的短链热路径在本机 `local-h2` 环境完成 1/2/4/8/16/32/64 阶梯压测：`/s/{shortCode}` 全部返回 `302` 且带 `Location`，最高阶 64 并发、每阶 128 请求时 P95 为 `179ms`，错误率 `0.00%`，访问事件队列水位 `0.00%`，没有丢弃事件或批量写失败。

这份报告证明“短链跳转主路径的本地回归质量良好”，不宣称生产 QPS。生产容量仍需要在 `Nginx + Spring Boot + MySQL + Redis` 的真实部署链路上重测。

## 本轮交付

| 事项 | 结果 |
| --- | --- |
| 短链热路径优化 | `last_visit_at` 从“每次访问都尝试 DB touch”改为 JVM 内按短码限频，减少热门短链热行更新 |
| 本地压测稳定性 | `application-local.yml` 的 H2 URL 增加 `DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`，避免长时间联调或连接池换连接后内存库丢表 |
| 短链 UI 表达 | 后台短链列表增加证据摘要、真实短链打开入口；短链详情页增加摘要指标和更清晰的标题层级 |
| 压测证据 | 新增 shortlink 专项报告和 mixed 回归报告，保留一次 readiness 失败记录作为硬风险修复证据 |

## 压测环境

| 项目 | shortlink 专项 |
| --- | --- |
| Report | [20260621-shortlink-redirect-optimized](performance-reports/20260621-shortlink-redirect-optimized/report.md) |
| Base URL | `http://127.0.0.1:48082` |
| Profile | `local-h2` |
| Workload | `shortlink` |
| 请求行为 | 不跟随跳转，只验证 `301/302 + Location` |
| 每阶样本 | `128` |
| 并发阶梯 | `1,2,4,8,16,32,64` |
| Readiness | `UP` |
| Runtime 观测 | `STRICT_RUNTIME_OBSERVATION=1` |
| 测试流量标记 | `X-Channel: perf-test`，Campaign 带 Run ID |

## 短链专项结果

| 并发 | 请求数 | RPS | Avg(ms) | P95(ms) | P99(ms) | 状态码 | 错误率 | 队列水位 | 事件丢弃 | 批量失败 |
| ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: |
| 1 | 128 | 175.58 | 4.65 | 7 | 12 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 2 | 128 | 284.44 | 5.86 | 10 | 15 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 4 | 128 | 374.27 | 9.43 | 13 | 14 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 8 | 128 | 395.06 | 18.67 | 26 | 28 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 16 | 128 | 402.52 | 36.52 | 46 | 53 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 32 | 128 | 393.85 | 72.09 | 90 | 93 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |
| 64 | 128 | 369.94 | 137.36 | 179 | 222 | 302 x 128 | 0.00% | 0.00% | 0 | 0 |

读法：

- `302 x 128` 说明每个请求都拿到了可跳转响应，脚本也检查了 `Location`。
- 队列水位始终是 `0.00%`，说明访问事件异步 worker 能及时排水。
- P95 随并发上升是本机线程调度和请求排队的正常现象；本轮没有触发 `STOP_P95_MS=1200`。

## 混合链路回归

| 项目 | mixed 回归 |
| --- | --- |
| Report | [20260621-mixed-after-shortlink-optimization](performance-reports/20260621-mixed-after-shortlink-optimization/report.md) |
| Workload | mixed，约 60% 短链、20% 结果读取、10% 后台 overview、10% health |
| 并发阶梯 | `1,4,8,16,32` |
| 每阶样本 | `120` |
| 结果 | 全阶段完成，最高整体 P95 `104ms`，错误率 `0.00%`，事件丢弃 `0`，批量失败 `0` |

最高阶 32 并发的分接口 P95：

| 接口类型 | P95(ms) | 说明 |
| --- | ---: | --- |
| shortlink | 90 | `/s/{code}` 302 热路径 |
| result | 55 | 结果读取 |
| admin | 113 | 后台 overview 聚合 |
| health | 3 | 健康检查 |

这个回归说明短链优化没有明显拖累结果页和后台查询；mixed 最高阶最慢接口是 `admin`，后续若继续优化整体体验，后台 overview 和聚合查询是更值得看的方向。

## 发现并修复的硬风险

第一次压测没有直接放行，脚本在 readiness 阶段拒跑，并写入：

[preflight-failed.json](performance-reports/20260621-shortlink-redirect-optimized/preflight-failed.json)

响应体显示所有核心表都 `unavailable`。结合后端日志里的 Hikari 时钟跳变软驱逐连接，可以判断本地 H2 内存库在连接关闭后丢失 schema。修复方式是让 local H2 数据库在连接关闭后继续保留：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:wuxing_local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

修复后 `/api/readiness` 返回 `UP`，核心表全部 `ok`，压测才继续执行。这是一个有价值的质量门案例：脚本没有在 schema 不可用时伪造“压测通过”。

## 优化说明

短链跳转的热路径目标是尽快完成：

```text
GET /s/{shortCode}
  -> 校验短码
  -> Redis 命中 shortCode -> resultId
  -> recordAsync 写入访问事件队列
  -> 低频 touch last_visit_at
  -> 302 /result/{resultId}?sc={shortCode}
```

本轮重点减少 `last_visit_at` 的同步 DB 压力。优化前，即使 Redis 已命中，每次短链访问仍会尝试执行一次条件 UPDATE。优化后，同一 JVM 内同一短码在配置窗口内只触发一次 touch；默认窗口来自：

```text
SHORT_LINK_LAST_VISIT_TOUCH_INTERVAL_SECONDS=30
```

这不会影响 302 跳转，也不会减少访问事件入队。它降低的是 `short_link.last_visit_at` 热行更新频率。

## 仍需诚实说明的边界

- 本轮是 `local-h2 + loopback`，不是生产 MySQL、Redis、Nginx、公网链路。
- JVM 限频是单实例内存记忆，多实例部署时每个实例每窗口仍可能 touch 一次。
- 应用重启后首个 hit 会重新 touch。
- 大量不同有效短码会让 `lastVisitTouchMillis` 常驻增长，后续可改为 Caffeine/Redis TTL 结构。
- Redis 异常时仍会回源 DB 查询 `short_link`，传播峰值叠加 Redis 抖动时 DB 会成为压力点。
- 访问事件队列保护跳转优先级；队列满时会丢低价值统计事件，不阻塞 302。因此报告必须分开说明“跳转成功率”和“统计完整性”。

## 下一步优化方向

| 优先级 | 方向 | 价值 |
| --- | --- | --- |
| P0 | 在 compose MySQL + Redis + Nginx 环境重跑 shortlink/mixed | 得到更接近上线链路的 P95、错误率和慢 SQL 证据 |
| P1 | 把 `lastVisitTouchMillis` 换成带 TTL/最大容量的本地缓存，或 Redis `SET NX EX` | 控制高基数短码下的内存增长，支持多实例限频 |
| P1 | 为 Redis 异常场景增加短链本地小缓存或熔断观测 | 降低 Redis 抖动时 DB 回源压力 |
| P1 | 对 admin overview 做更细的日聚合和缓存命中率记录 | mixed 回归里后台 overview 是最高阶最慢接口 |
| P2 | RocketMQ consumer 真正接管访问事件落库后重压 | 验证统计链路从本地队列升级到 MQ 后的吞吐 |
| P2 | 加入 Nginx 连接、JVM GC、Hikari、MySQL slow log 采集 | 把 P95 拐点定位到网络、线程、连接池或 SQL |

## 复现命令

短链专项：

```bash
env BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token \
WORKLOAD=shortlink STEPS=1,2,4,8,16,32,64 REQUESTS_PER_STAGE=128 \
RUN_ID=20260621-shortlink-redirect-optimized \
OUT_DIR=docs/performance-reports/20260621-shortlink-redirect-optimized \
STRICT_RUNTIME_OBSERVATION=1 DEPLOYMENT_PROFILE=local-h2 \
scripts/performance-limit-test.sh
```

混合回归：

```bash
env BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token \
WORKLOAD=mixed STEPS=1,4,8,16,32 REQUESTS_PER_STAGE=120 \
RUN_ID=20260621-mixed-after-shortlink-optimization \
OUT_DIR=docs/performance-reports/20260621-mixed-after-shortlink-optimization \
STRICT_RUNTIME_OBSERVATION=1 DEPLOYMENT_PROFILE=local-h2 \
scripts/performance-limit-test.sh
```
