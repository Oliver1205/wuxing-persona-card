# 五行人格卡源码阅读手册 2026-06-21

## 怎么读这个项目

这个项目表面是人格测试 H5，工程主线其实是短链接传播闭环：

```text
生成结果 -> 绑定短码 -> 分享 /s/{code} -> 302 回结果页 -> 访问事件入库 -> 后台看 PV / UV / UIP
```

建议按下面 5 条路线读源码：

| 路线 | 先看什么 | 看完要能解释什么 |
| --- | --- | --- |
| 短链入口 | `ShortLinkController` | `/s/{code}` 为什么能 302 到结果页 |
| Provider 架构 | `ShortLinkService` | internal / external 短链怎么切换 |
| 缓存降压 | `InternalShortLinkProvider`、`RedisCacheService` | 热门短链如何少查 DB、如何防空短码穿透 |
| 统计链路 | `VisitEventService` | 为什么统计不阻塞跳转，队列满会怎样 |
| 后台展示 | `AdminDashboard.vue`、`AdminShortLinkDetail.vue` | 运营台怎么看短链回流、口径和访问明细 |

## 1. 短链跳转入口

文件：`backend/src/main/java/com/wuxing/persona/controller/ShortLinkController.java`

这段代码是短链项目最核心的入口。浏览器访问 `/s/{shortCode}` 后，后端不渲染页面，而是解析短码，再返回 `302 Location`。

```java
@GetMapping("/s/{shortCode}")
public void redirect(@PathVariable String shortCode,
                     @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                     HttpServletRequest request,
                     HttpServletResponse response) {
    String resultId = shortLinkService.resolveAndRecord(shortCode, clientId, request);
    if (resultId == null) {
        redirectTo(response, "/not-found");
        return;
    }
    redirectTo(response, resultLocation(resultId, shortCode, request));
}

private String resultLocation(String resultId, String shortCode, HttpServletRequest request) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/result/" + resultId)
            .queryParam("sc", shortCode);
    appendQueryParam(builder, "channel", firstPresent(request.getParameter("channel"),
            request.getParameter("utm_source"), request.getParameter("source")));
    appendQueryParam(builder, "campaign", firstPresent(request.getParameter("campaign"),
            request.getParameter("utm_campaign")));
    return builder.build().toUriString();
}
```

重点：

- `resolveAndRecord` 同时做两件事：解析短码、记录访问事件。
- 找不到短码时跳 `/not-found`，不把异常直接抛给用户。
- `channel`、`campaign`、`utm_source`、`utm_campaign` 会透传到结果页，方便后台归因。

## 2. Provider 架构

文件：`backend/src/main/java/com/wuxing/persona/service/ShortLinkService.java`

`ShortLinkService` 不自己生成短码，而是根据配置选择 Provider。这样 internal 短链和 external 短链可以共用业务入口。

```java
@Service
public class ShortLinkService {

    private final AppProperties appProperties;
    private final InternalShortLinkProvider internalShortLinkProvider;
    private final ExternalShortLinkProvider externalShortLinkProvider;

    public ShortLinkEntity createForResult(String resultId) {
        return activeProvider().createForResult(resultId);
    }

    public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
        return activeProvider().resolveAndRecord(shortCode, clientId, request);
    }

    ShortLinkProvider activeProvider() {
        ShortLinkMode mode = ShortLinkMode.from(appProperties.getShortLink().getMode());
        if (mode == ShortLinkMode.EXTERNAL) {
            return externalShortLinkProvider;
        }
        return internalShortLinkProvider;
    }
}
```

重点：

- `SHORT_LINK_MODE=internal` 时使用本项目内置短码。
- `SHORT_LINK_MODE=external` 时对接独立短链服务。
- Controller 和 ResultService 不需要知道当前是哪一种实现。

## 3. 短码生成和绑定

文件：`backend/src/main/java/com/wuxing/persona/service/shortlink/InternalShortLinkProvider.java`

创建结果时，如果这个 `resultId` 已经有短链，就直接复用；否则生成 6 位 Base62 短码，插入 `short_link`，再写入 Redis。

```java
@Override
public ShortLinkEntity createForResult(String resultId) {
    ShortLinkEntity existing = shortLinkMapper.selectByResultId(resultId);
    if (existing != null) {
        return existing;
    }
    for (int i = 0; i < ShortLinkCodeUtils.MAX_RETRY; i++) {
        String shortCode = randomCode();
        ShortLinkEntity entity = buildEntity(resultId, shortCode, appProperties.getBaseUrl() + "/s/" + shortCode);
        try {
            shortLinkMapper.insert(entity);
            redisCacheService.setShortLinkResultId(shortCode, resultId);
            return entity;
        } catch (DuplicateKeyException ex) {
            log.warn("Short code collision, retrying, resultId={}, attempt={}", resultId, i + 1);
        }
    }
    throw new BusinessException("short code generation failed, please retry");
}
```

重点：

- `selectByResultId` 保证同一个结果不会生成一堆重复短链。
- `DuplicateKeyException` 是短码冲突兜底；冲突时重试，不直接失败。
- `shortUrl` 由 `app.base-url + /s/{code}` 生成，前端和后台都展示这个真实传播入口。

## 4. 短链解析和缓存降压

文件：`backend/src/main/java/com/wuxing/persona/service/shortlink/InternalShortLinkProvider.java`

短链跳转时，先看空值缓存，再看 `shortCode -> resultId` 缓存，只有缓存未命中才查 DB。

```java
@Override
public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
    ShortLinkCodeUtils.validate(shortCode);
    if (redisCacheService.isNullShortLink(shortCode)) {
        return null;
    }
    String resultId = redisCacheService.getShortLinkResultId(shortCode);
    ShortLinkEntity entity = null;
    if (resultId == null || resultId.isBlank()) {
        entity = shortLinkMapper.selectByShortCode(shortCode);
        if (entity == null) {
            redisCacheService.setNullShortLink(shortCode);
            return null;
        }
        resultId = entity.getResultId();
        redisCacheService.setShortLinkResultId(shortCode, resultId);
    }
    visitEventService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/" + shortCode, resultId, shortCode,
            clientId, request);
    touchLastVisitAtIfStale(shortCode);
    return resultId;
}
```

重点：

- `ShortLinkCodeUtils.validate(shortCode)` 先挡掉非法短码。
- 无效短码写入 `shortlink:null:{code}`，避免重复打 DB。
- 有效短码命中 Redis 时可以直接拿到 `resultId`。
- 访问事件用 `recordAsync`，不会同步聚合 PV/UV/UIP。

## 5. 本轮性能优化点

文件：`backend/src/main/java/com/wuxing/persona/service/shortlink/InternalShortLinkProvider.java`

优化前，Redis 命中后仍会每次尝试执行 `touchLastVisitAtIfStale` 的 DB 更新。优化后，同一 JVM 内同一短码在配置窗口内只 touch 一次。

```java
private void touchLastVisitAtIfStale(String shortCode) {
    long nowMillis = System.currentTimeMillis();
    int intervalSeconds = appProperties.getShortLink().getLastVisitTouchIntervalSeconds();
    if (!shouldTouchLastVisitAt(shortCode, nowMillis, intervalSeconds)) {
        return;
    }
    LocalDateTime now = LocalDateTime.now();
    try {
        shortLinkMapper.touchLastVisitAtIfStale(shortCode, now, now.minusSeconds(intervalSeconds));
    } catch (RuntimeException ex) {
        lastVisitTouchMillis.remove(shortCode, nowMillis);
        log.warn("Short link last visit touch failed, shortCode={}, error={}: {}",
                shortCode, ex.getClass().getSimpleName(), ex.getMessage());
    }
}

private boolean shouldTouchLastVisitAt(String shortCode, long nowMillis, int intervalSeconds) {
    if (intervalSeconds <= 0) {
        return true;
    }
    long intervalMillis = intervalSeconds * 1000L;
    boolean[] shouldTouch = {false};
    lastVisitTouchMillis.compute(shortCode, (key, previousMillis) -> {
        if (previousMillis == null || nowMillis - previousMillis >= intervalMillis) {
            shouldTouch[0] = true;
            return nowMillis;
        }
        return previousMillis;
    });
    return shouldTouch[0];
}
```

重点：

- 这是减少 `short_link.last_visit_at` 热行更新，不是减少访问事件。
- DB 更新失败只打 warn，不影响 302。
- 失败时移除本地时间戳，下一次访问可以重试。
- 多实例场景仍然是“每个实例每窗口最多一次”，不是全局分布式限频。

## 6. Redis 缓存策略

文件：`backend/src/main/java/com/wuxing/persona/service/RedisCacheService.java`

这段代码体现了本项目对缓存的态度：缓存失败不影响主流程，但会记录 warning。

```java
private static final Duration RESULT_TTL = Duration.ofHours(24);
private static final Duration SHORT_LINK_TTL = Duration.ofDays(7);
private static final Duration NULL_SHORT_LINK_TTL = Duration.ofMinutes(5);
private static final Duration ADMIN_OVERVIEW_TTL = Duration.ofSeconds(45);

public String getShortLinkResultId(String shortCode) {
    try {
        return redisTemplate.opsForValue().get(shortLinkKey(shortCode));
    } catch (Exception ex) {
        log.warn("Read short link cache failed, shortCode={}", shortCode);
        return null;
    }
}

public void setNullShortLink(String shortCode) {
    try {
        redisTemplate.opsForValue().set(nullShortLinkKey(shortCode), "1", NULL_SHORT_LINK_TTL);
    } catch (Exception ex) {
        log.warn("Write null short link cache failed, shortCode={}", shortCode);
    }
}
```

重点：

- 结果缓存 24 小时，短链映射 7 天。
- 无效短码空值缓存 5 分钟。
- 后台 overview 只缓存 45 秒，既降低查询压力，又不让运营数据长期过期。
- Redis 出问题时短链仍能回源 DB，但高峰期 DB 压力会升高。

## 7. 访问事件异步写入

文件：`backend/src/main/java/com/wuxing/persona/service/VisitEventService.java`

短链跳转优先保护用户体验，访问事件进入有界队列，后台 worker 批量落库。

```java
public void recordAsync(EventType eventType,
                        String pagePath,
                        String resultId,
                        String shortCode,
                        String clientId,
                        HttpServletRequest request,
                        String sessionId,
                        String channel,
                        String campaign) {
    VisitEventEntity entity = buildEntity(eventType, pagePath, resultId, shortCode, clientId, request,
            sessionId, channel, campaign);
    if (appProperties.getVisitEvent().isRocketMqMode()) {
        if (publishToRocketMq(entity, eventType, pagePath, resultId, shortCode)) {
            if (shouldUseRocketMqConsumerPersistence()) {
                return;
            }
            rocketMqShadowLocalEvents.incrementAndGet();
            enqueueLocal(entity, eventType, pagePath, resultId, shortCode);
            return;
        }
        if (!appProperties.getVisitEvent().getRocketmq().isFallbackToLocal()) {
            long dropped = droppedAsyncEvents.incrementAndGet();
            log.warn("Visit event RocketMQ publish failed and local fallback is disabled, dropped={}, eventType={}, pagePath={}, resultId={}, shortCode={}",
                    dropped, eventType, pagePath, resultId, shortCode);
            return;
        }
        rocketMqFallbackEvents.incrementAndGet();
    }
    enqueueLocal(entity, eventType, pagePath, resultId, shortCode);
}

private void enqueueLocal(VisitEventEntity entity,
                          EventType eventType,
                          String pagePath,
                          String resultId,
                          String shortCode) {
    if (!asyncQueue.offer(entity)) {
        long dropped = droppedAsyncEvents.incrementAndGet();
        log.warn("Visit event async queue full, dropped={}, eventType={}, pagePath={}, resultId={}, shortCode={}",
                dropped, eventType, pagePath, resultId, shortCode);
    }
}
```

重点：

- 默认本地队列，后续可切 RocketMQ。
- 队列满时丢统计事件，不阻塞用户跳转。
- 运行态会暴露 `queueSize`、`droppedAsyncEvents`、`batchWriteFailures`。
- 压测报告里必须同时看 HTTP 成功率和统计完整性。

worker 的批量写入如下：

```java
private void drainAsyncEvents() {
    List<VisitEventEntity> batch = new ArrayList<>(asyncDrainLimit);
    while (running.get()) {
        try {
            VisitEventEntity first = asyncQueue.take();
            batch.add(first);
            asyncQueue.drainTo(batch, asyncDrainLimit - 1);
            flushBatch(batch);
            batch.clear();
        } catch (InterruptedException ex) {
            if (!running.get()) {
                Thread.currentThread().interrupt();
                break;
            }
        } catch (RuntimeException ex) {
            log.warn("Visit event async worker failed, error={}: {}", ex.getClass().getSimpleName(),
                    ex.getMessage());
            batch.clear();
        }
    }
}
```

## 8. 本地联调可靠性

文件：`backend/src/main/resources/application-local.yml`

本轮压测前第一次被 readiness 拦住，原因是 H2 内存库在连接池换连接后丢表。当前 local URL 已修复：

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:wuxing_local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
```

重点：

- `DB_CLOSE_DELAY=-1` 让内存库不因最后一个连接关闭而立刻销毁。
- `DB_CLOSE_ON_EXIT=FALSE` 避免 JVM 退出时 H2 自动做额外关闭流程。
- 这只影响 local profile，不影响生产 MySQL。

## 9. 后台短链证据展示

文件：`frontend/src/pages/AdminDashboard.vue`

后台不只是看总 PV，而是把短链总量、回流访问、当前页 PV 和统计口径放成摘要。

```ts
const shortLinkEvidenceCards = computed(() => {
  const records = shortLinks.value?.records ?? [];
  const pagePv = records.reduce((sum, item) => sum + item.pv, 0);
  const created = overview.value?.shortLinkCreated ?? 0;
  const visits = overview.value?.shortLinkVisits ?? 0;
  const sourceFilterLabel = statSource.value === 'external'
    ? '只看外部'
    : statSource.value === 'local'
      ? '只看本地'
      : '本地 + 外部';
  return [
    {
      label: '短链总量',
      value: String(shortLinks.value?.total ?? 0),
      note: '当前筛选范围',
    },
    {
      label: '回流访问',
      value: String(visits),
      note: `${perShortLinkLabel(averagePerItem(visits, created))} / 条短链`,
    },
    {
      label: '当前页 PV',
      value: String(pagePv),
      note: `${shortLinkStartIndex.value}-${shortLinkEndIndex.value} 条明细`,
    },
    {
      label: '统计口径',
      value: overview.value ? metricSourceLabel(overview.value.metricSource) : '待加载',
      note: sourceFilterLabel,
    },
  ];
});
```

模板里同时提供真实短链打开入口：

```vue
<a class="shortlink-url" :href="item.shortUrl" target="_blank" rel="noopener noreferrer">
  打开
</a>
```

重点：

- `statSource` 区分本地统计和外部短链平台统计。
- `metricSource` 区分实时事件、日聚合和外部统计。
- “打开”链接让验收者能直接验证 `/s/{code}` 的 302。

## 10. 读完以后你应该能讲清楚

- 为什么这个项目的核心不是“测五行”，而是“短链传播闭环”。
- `/s/{code}` 的返回为什么是 302，不是 JSON。
- Redis 在短链里分别承担有效映射缓存和无效短码空值缓存。
- 为什么访问事件要异步写，队列满时为什么宁可丢统计也不阻塞跳转。
- `last_visit_at` 限频优化降低了哪一类 DB 压力，又没有解决哪些分布式问题。
- 后台为什么要标出 `statSource` 和 `metricSource`，避免把本地实时数据、日聚合数据、外部短链数据混在一起。
- 本机压测报告能证明什么，不能证明什么。
