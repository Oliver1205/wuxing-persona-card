# 五行人格项目阶梯压测记录

- Base URL: `http://127.0.0.1:18080`
- Started: `2026-06-14T02:07:19`
- Workload: `health`
- Result ID: `R20260614020720012976889`
- Short Code: `kaVFM1`
- Stop condition: P95 > `1200ms` or error rate > `5.00%`
- Stop reason: **completed all stages**

| 并发 | 请求数 | RPS | Avg(ms) | P50 | P95 | P99 | 错误率 | 队列 | 落库增量 | 丢弃增量 | 健康 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 10 | 2000.0 | 0 | 0 | 0 | 0 | 0.00% | 0 | 0 | 0 | ok |
| 2 | 10 | 2500.0 | 0.1 | 0 | 1 | 1 | 0.00% | 0 | 0 | 0 | ok |

## 分接口 P95

| 并发 | shortlink | result | admin | health |
| ---: | ---: | ---: | ---: | ---: |
| 1 | - | - | - | 0 |
| 2 | - | - | - | 1 |

## 解读口径

- `WORKLOAD=mixed` 约为 60% 短链、20% 结果读取、10% 后台 overview、10% 健康检查。
- `WORKLOAD=shortlink` 不跟随跳转，只验证 301/302 和 Location，更接近短链热路径本身。
- `WORKLOAD=result|admin|health` 用于分别观察结果读取、后台查询和服务健康检查的单路径边界。
- 若 P95 上升但队列不积压，瓶颈更可能在 HTTP/DB 查询；若队列积压或丢弃增加，瓶颈在访问事件异步写入。
- 本脚本适合单机阶梯探测，不替代生产级分布式压测。
