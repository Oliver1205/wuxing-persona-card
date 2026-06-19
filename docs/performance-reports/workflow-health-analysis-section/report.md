# 五行人格项目阶梯压测记录

## 环境卡片

- Run ID: `workflow-health-analysis-section`
- Git SHA: `e317d64`
- Git state: `dirty`
- Java: `openjdk version 17.0.19 2026-04-21`
- Python: `Python 3.9.6`
- Host: `linyuxiangdeMacBook-Air.local`
- Deployment profile: `local-h2`
- Base URL: `http://127.0.0.1:48081`
- Started: `2026-06-14T06:24:06`
- Workload: `health`
- Synthetic Channel: `perf-test`
- Synthetic Campaign: `performance-limit-test`
- Effective Synthetic Campaign: `performance-limit-test:workflow-health-analysis-section`
- Public target allowed: `False`
- RocketMQ shadow load allowed: `False`
- Stage cooldown seconds: `0`
- Preflight async mode: `local`
- Preflight runtime health: `ok`
- Preflight RocketMQ available: `False`
- Preflight MQ consumer ready: `False`
- Preflight local fallback: `True`
- Result ID: `R20260614062406582446339`
- Short Code: `na21En`
- Stop condition: P95 > `1200ms`, error rate > `5.00%`, queue usage >= `90.00%`, dropped async events > `0`, batch write failures > `0`, or runtime danger = `True`
- Stop reason: **completed all stages**

## 自动结论与下一步

- 本轮完成所有阶段，最高 P95 为 0ms，最高错误率为 0.00%。
- 最高并发阶段最慢接口类型是 `health`，P95 为 0ms，错误数 0。
- 访问事件运行态未观察到丢弃或批量写失败，最高队列水位 0.00%。
- 这是 `health` 小流量格式验证，只证明脚本、环境卡片和 runtime 采集正常，不代表业务链路容量。
- 目标是本机 loopback，结论只能作为本地回归和脚本格式证据；公网容量需要在备案/授权后用真实 Nginx、MySQL、Redis 链路重测。

| 并发 | 请求数 | RPS | Avg(ms) | P50 | P95 | P99 | 错误率 | 队列 | 队列水位 | 落库增量 | 丢弃增量 | 批量失败增量 | 健康 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 4 | 1333.33 | 0 | 0 | 0 | 0 | 0.00% | 0 | 0.00% | 0 | 0 | 0 | ok |

## 分接口 P95

| 并发 | shortlink | result | admin | health |
| ---: | ---: | ---: | ---: | ---: |
| 1 | - | - | - | 0 |

## 解读口径

- `WORKLOAD=mixed` 约为 60% 短链、20% 结果读取、10% 后台 overview、10% 健康检查。
- `WORKLOAD=shortlink` 不跟随跳转，只验证 301/302 和 Location，更接近短链热路径本身。
- `WORKLOAD=result|admin` 用于分别观察结果读取和后台查询的单路径边界。
- `WORKLOAD=health` 仍会先执行脚本链路预检和 seed result，再进行 health 小流量请求，不是纯健康接口压测。
- 公网目标会要求 `ALLOW_PUBLIC_LOAD_TEST=1`、每阶样本数至少为最大配置并发的 2 倍，并记录 `DEPLOYMENT_PROFILE`。
- 公网多阶压测要求 `STAGE_COOLDOWN_SECONDS>=30`，避免队列、连接池或 JVM 状态未冷却就进入下一阶。
- 公网且 `asyncMode=rocketmq` 时，如果 MQ 不可用或 consumer 未就绪，默认拒绝压测；只有显式设置 `ALLOW_ROCKETMQ_SHADOW_LOAD_TEST=1` 才测试 shadow/fallback。
- 若 P95 上升但队列不积压，瓶颈更可能在 HTTP/DB 查询；若队列积压或丢弃增加，瓶颈在访问事件异步写入。
- 本脚本适合单机阶梯探测，不替代生产级分布式压测。
