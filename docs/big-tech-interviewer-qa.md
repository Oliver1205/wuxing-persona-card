# 大厂面试官压力追问清单

这份清单用于把项目讲法从“我做了一个页面”推进到“我能解释一个真实业务闭环的工程取舍”。每个问题都按三个角度准备：

- 面试官在考什么。
- 仓库里有什么证据。
- 回答时要主动承认什么边界。

最稳的总口径：

> 这是一个单机商业化作品，不是完整分布式短链平台。我能讲清楚热路径、缓存、异步事件、降级和验证证据，但不会声称它已经通过生产级 QPS 压测。

## 1. 短链跳转为什么算热路径？

面试官在考：你是否知道哪个接口最容易被真实流量打爆。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/controller/ShortLinkController.java`
- `backend/src/main/java/com/wuxing/persona/service/InternalShortLinkProvider.java`
- `backend/src/main/java/com/wuxing/persona/mapper/ShortLinkMapper.java`

回答要点：

- 用户完成测试后会复制或分享短链，朋友访问都进入 `/s/{shortCode}`。
- 群聊传播时，结果创建可能只有一次，但短链跳转会被放大很多次。
- 当前跳转链路的目标是快速解析 shortCode 并返回 302，不在请求线程里做 PV、UV、UIP 实时聚合。
- 短码映射优先走 Redis，未命中才查 MySQL，并且不存在的短码会有短 TTL 空值缓存防穿透。

边界要主动说：

- 当前仍是单机作品，不等于多机房短链系统。
- 如果 Redis 命中后仍需要补充 DB 实体字段，后续还可以继续把跳转所需最小数据缓存成完整快照。
- 真正高并发还需要线上压测、告警、限流阈值和容量评估。

## 2. 异步访问事件为什么不用 MQ？

面试官在考：你是否理解“异步化”和“引入中间件”的区别。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/service/VisitEventService.java`
- `backend/src/test/java/com/wuxing/service/VisitEventServiceTest.java`
- `backend/src/test/java/com/wuxing/service/InternalShortLinkProviderTest.java`

回答要点：

- 短链访问事件是统计事实来源，但不应该阻塞用户跳转。
- 当前采用有界内存队列和后台 daemon worker，把短链访问事件从请求线程挪出。
- 队列满时会记录 warn 并丢弃事件，主链路继续返回 302。
- 这适合单机 MVP 和作品集阶段，因为运维复杂度低，代码证据清楚。

边界要主动说：

- 进程重启可能丢失未刷入数据库的队列事件。
- 没有消息重试、死信队列和跨实例消费协调。
- 如果流量继续上升，下一步应该用 MQ 或日志管道承接事件，再批量落库。

## 3. 后台统计怎么避免主库被运营刷新打爆？

面试官在考：你是否区分用户核心链路和运营查询链路。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/service/AdminStatService.java`
- `backend/src/main/java/com/wuxing/persona/service/AnalyticsAggregationService.java`
- `backend/src/main/java/com/wuxing/persona/service/RedisCacheService.java`

回答要点：

- 后台总览不是金融交易，不需要每一次刷新都强实时。
- overview 可以用 45 秒 Redis 短缓存，减少运营反复刷新造成的重复聚合。
- 趋势查询限制日期范围，历史日期优先读 `site_daily_metric` 和 `short_link_daily_metric`。
- 短链列表按 page 拉短链，再批量补结果信息和统计，避免一页触发 N+1 查询。

边界要主动说：

- 当日期范围包含今天时，仍需要读取实时事件。
- 日聚合当前主要靠手动或测试链路验证，生产阶段应补定时任务和补偿机制。
- 缓存只能削峰，不能替代索引、归档和慢查询治理。

## 4. external stats 缓存解决什么？为什么不是 Redis？

面试官在考：你是否能解释一个看似小优化背后的成本收益。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/service/ExternalShortLinkStatsAdapter.java`
- `backend/src/test/java/com/wuxing/service/ExternalShortLinkStatsAdapterTest.java`
- `docs/external-shortlink-integration-guide.md`

回答要点：

- external 模式下，后台短链列表需要向外部短链服务拉 PV、UV、UIP。
- 运营刷新列表时，同一日期范围的外部统计可能被重复请求。
- 当前用 JVM 本地 TTL 缓存保护外部 HTTP 调用，默认 60 秒，可通过配置关闭。
- 失败结果不缓存，避免把临时错误固化。

边界要主动说：

- 本地缓存不是多实例共享缓存。
- 当前没有容量上限和淘汰策略，流量更大时应换成 Caffeine 或 Redis。
- external 访问明细仍然依赖外部接口，不能因为 stats cache 就声称全链路无外部压力。

## 5. Redis 挂了会不会全站不可用？

面试官在考：你是否把缓存当成唯一真相。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/service/RedisCacheService.java`
- `backend/src/test/java/com/wuxing/service/RedisCacheServiceTest.java`
- `backend/src/main/java/com/wuxing/persona/service/ResultService.java`

回答要点：

- Redis 是削峰层，不是权威数据源。
- 结果详情缓存读取失败可以回源 `user_result` 和 `short_link`。
- 短码映射缓存失败可以回源 `short_link`。
- 写缓存失败记录 warn，不阻断创建结果、查看结果和短链跳转。

边界要主动说：

- Redis 故障会把压力转回 MySQL，不能假装没有影响。
- 生产阶段需要限流、熔断、告警和热点 key 观测。
- 如果希望进一步稳定，可以补本地二级缓存或短期快照缓存。

## 6. 数据库索引是怎么围绕查询设计的？

面试官在考：你是否只是“加了缓存”，还是理解查询路径。

仓库证据：

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/schema-local.sql`
- `backend/src/main/java/com/wuxing/persona/mapper/VisitEventMapper.java`
- `backend/src/main/java/com/wuxing/persona/mapper/ShortLinkMapper.java`

回答要点：

- `visit_event` 是后台统计的事实表，日期范围、事件类型、shortCode、clientIdHash、ipHash 都会影响查询。
- `short_link` 列表按状态和创建时间分页，所以需要围绕 `status + created_at` 设计索引。
- 日聚合表承担历史趋势和短链历史统计，避免每次都扫明细。
- 查询优化不是只看单条 SQL，而是看页面一次刷新会触发多少次数据库往返。

边界要主动说：

- `COUNT DISTINCT` 在大数据量下仍然昂贵。
- 明细表增长后需要分区、归档或离线聚合。
- 还没有正式的生产慢查询报告，不能宣称已经解决所有数据库瓶颈。

## 7. 为什么还没有 Flyway 或 Liquibase？

面试官在考：你是否知道脚本初始化和生产迁移不是一回事。

仓库证据：

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/schema-local.sql`
- `backend/src/main/resources/application.yml`
- `docs/deploy.md`

回答要点：

- 当前仓库以单机部署和作品集演示为主，保留了 schema 初始化脚本和本地 H2 脚本。
- 这能保证新环境快速拉起，但不等于成熟生产迁移体系。
- 如果进入长期生产迭代，应引入 Flyway 或 Liquibase，按版本管理 DDL，避免手工改库。

边界要主动说：

- `continue-on-error` 一类配置适合兼容已有环境，不适合作为严肃迁移方案。
- 生产环境要有回滚脚本、备份和灰度迁移策略。

## 8. external 短链服务失败时主链路怎么保？

面试官在考：你是否理解外部依赖会扩大故障面。

仓库证据：

- `backend/src/main/java/com/wuxing/persona/service/ExternalShortLinkProvider.java`
- `backend/src/main/java/com/wuxing/persona/service/RestExternalShortLinkClient.java`
- `backend/src/main/java/com/wuxing/persona/config/AppProperties.java`
- `docs/external-shortlink-integration-guide.md`

回答要点：

- 短链 Provider 把 internal 和 external 适配隔离起来。
- external 创建失败时可以配置降级 internal，让用户仍然拿到可访问结果。
- 本地 `short_link` 仍保留五行业务 resultId 和 shortUrl 绑定，便于后台统计和兼容跳转。

边界要主动说：

- 如果外部 HTTP 调用在核心事务里耗时过长，会影响结果创建体验。
- 生产阶段应配置超时、重试、熔断和更清晰的事务边界。
- 降级 internal 后，外部平台侧统计不会天然完整，需要后台口径说明。

## 9. 前端产品化做了哪些取舍？

面试官在考：你是否理解体验改动背后的用户心理。

仓库证据：

- `frontend/src/pages/GuidePage.vue`
- `frontend/src/pages/TestPage.vue`
- `frontend/src/pages/ResultPage.vue`
- `frontend/src/components/ShareLinkBox.vue`

回答要点：

- 首页承诺和测试页输入保持一致，默认只要求出生年月和 5 道题。
- 日、时辰是可选增强信息，折叠起来，减少第一步压力。
- 逐题卡片式问答降低信息噪音。
- 选中答案后不会瞬间切走，而是保留 1100 ms 确认反馈，并允许手动点下一题。
- 结果页围绕“保存图、复制短链、朋友访问”展开，不只展示静态文案。

边界要主动说：

- 目前主要是本地浏览器和移动视口验证，还缺真实手机、微信内置浏览器和用户访谈证据。
- 前端体验不能只靠主观判断，下一步应做截图基线和小规模用户测试。

## 10. 你真实做过线上压测吗？能说 QPS 吗？

面试官在考：你是否会为了包装项目而夸大。

仓库证据：

- `scripts/performance-smoke-test.sh`
- `scripts/quality-check.sh`
- `docs/quality-scorecard.md`
- `docs/eight-hour-completion-audit.md`

回答要点：

- 当前有性能 smoke，用真实创建结果、短链访问和后台 overview 读取观察是否明显退化。
- 这属于回归检查，不是生产压测报告。
- 面试中可以讲优化思路、代码证据和本地 smoke 结果，但不要宣称已验证某个生产 QPS。

边界要主动说：

- 生产 QPS 需要固定机器规格、数据规模、并发模型、压测工具、指标和报告。
- 后续可用 k6、wrk 或 Gatling 做分层压测：短链跳转、结果查询、事件上报、后台统计分开测。
- 压测之后还要看 CPU、内存、连接池、慢 SQL、Redis 命中率和错误率。

## 防翻车三句话

1. 这是单机商业化作品，不是完整分布式短链平台。
2. 我能讲热路径、降级和证据，但不声称已验证生产 QPS。
3. 下一步是线上压测、告警、Flyway、MQ 或日志管道、真实设备截图和用户访谈。
