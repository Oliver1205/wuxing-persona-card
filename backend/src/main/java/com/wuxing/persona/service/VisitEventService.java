package com.wuxing.persona.service;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.util.IpUtils;
import com.wuxing.persona.vo.VisitEventRuntimeVO;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VisitEventService {

    private static final Logger log = LoggerFactory.getLogger(VisitEventService.class);
    private static final int MAX_PAGE_PATH_LENGTH = 255;
    private static final int MAX_RESULT_ID_LENGTH = 64;
    private static final int MAX_SHORT_CODE_LENGTH = 32;
    private static final int MAX_REFERER_LENGTH = 255;
    private static final int MAX_ATTRIBUTION_LENGTH = 64;

    private final VisitEventMapper visitEventMapper;
    private final AppProperties appProperties;
    private final VisitEventRocketMqPublisher rocketMqPublisher;
    private final int asyncQueueCapacity;
    private final int asyncDrainLimit;
    private final BlockingQueue<VisitEventEntity> asyncQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong droppedAsyncEvents = new AtomicLong();
    private final AtomicLong totalFlushedEvents = new AtomicLong();
    private final AtomicLong batchWriteFailures = new AtomicLong();
    private final AtomicLong rocketMqPublishedEvents = new AtomicLong();
    private final AtomicLong rocketMqPublishFailures = new AtomicLong();
    private final AtomicLong rocketMqFallbackEvents = new AtomicLong();
    private final AtomicLong rocketMqShadowLocalEvents = new AtomicLong();
    private final AtomicInteger lastBatchSize = new AtomicInteger();
    private final Thread asyncWorker;
    private volatile LocalDateTime lastFlushAt;

    public VisitEventService(VisitEventMapper visitEventMapper,
                             AppProperties appProperties,
                             VisitEventRocketMqPublisher rocketMqPublisher) {
        this.visitEventMapper = visitEventMapper;
        this.appProperties = appProperties;
        this.rocketMqPublisher = rocketMqPublisher;
        this.asyncQueueCapacity = appProperties.getVisitEvent().getAsyncQueueCapacity();
        this.asyncDrainLimit = appProperties.getVisitEvent().getAsyncDrainLimit();
        this.asyncQueue = new LinkedBlockingQueue<>(asyncQueueCapacity);
        this.asyncWorker = new Thread(this::drainAsyncEvents, "visit-event-async-writer");
        this.asyncWorker.setDaemon(true);
        this.asyncWorker.start();
    }

    public void record(EventType eventType,
                       String pagePath,
                       String resultId,
                       String shortCode,
                       String clientId,
                       HttpServletRequest request) {
        record(eventType, pagePath, resultId, shortCode, clientId, request, null, null, null);
    }

    public void record(EventType eventType,
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
        insertWithDegrade(entity, eventType, pagePath, resultId, shortCode);
    }

    public void recordAsync(EventType eventType,
                            String pagePath,
                            String resultId,
                            String shortCode,
                            String clientId,
                            HttpServletRequest request) {
        recordAsync(eventType, pagePath, resultId, shortCode, clientId, request, null, null, null);
    }

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
        if (appProperties.getVisitEvent().isSyncMode()) {
            recordSynchronously(entity, eventType, pagePath, resultId, shortCode);
            return;
        }
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

    public VisitEventRuntimeVO runtime() {
        VisitEventRuntimeVO runtime = new VisitEventRuntimeVO();
        runtime.setQueueSize(asyncQueue.size());
        runtime.setQueueCapacity(asyncQueueCapacity);
        runtime.setDrainLimit(asyncDrainLimit);
        runtime.setDroppedAsyncEvents(droppedAsyncEvents.get());
        runtime.setTotalFlushedEvents(totalFlushedEvents.get());
        runtime.setLastFlushAt(lastFlushAt);
        runtime.setLastBatchSize(lastBatchSize.get());
        runtime.setBatchWriteFailures(batchWriteFailures.get());
        runtime.setWorkerAlive(asyncWorker.isAlive());
        runtime.setAsyncMode(appProperties.getVisitEvent().getAsyncMode());
        runtime.setRocketMqAvailable(rocketMqPublisher.isAvailable());
        runtime.setRocketMqFallbackToLocal(appProperties.getVisitEvent().getRocketmq().isFallbackToLocal());
        runtime.setRocketMqTopic(appProperties.getVisitEvent().getRocketmq().getTopic());
        runtime.setRocketMqPublishedEvents(rocketMqPublishedEvents.get());
        runtime.setRocketMqPublishFailures(rocketMqPublishFailures.get());
        runtime.setRocketMqFallbackEvents(rocketMqFallbackEvents.get());
        runtime.setRocketMqShadowLocalEvents(rocketMqShadowLocalEvents.get());
        runtime.setRocketMqConsumerEnabled(appProperties.getVisitEvent().getRocketmq().isConsumerEnabled());
        runtime.setRocketMqConsumerPersistenceReady(rocketMqPublisher.isConsumerPersistenceReady());
        applyHealth(runtime);
        return runtime;
    }

    private boolean shouldUseRocketMqConsumerPersistence() {
        return appProperties.getVisitEvent().getRocketmq().isConsumerEnabled()
                && rocketMqPublisher.isConsumerPersistenceReady();
    }

    private void applyHealth(VisitEventRuntimeVO runtime) {
        int queueUsagePercent = runtime.getQueueCapacity() <= 0
                ? 0
                : (int) Math.round(runtime.getQueueSize() * 100.0 / runtime.getQueueCapacity());
        if (!runtime.isWorkerAlive()) {
            runtime.setHealthStatus("danger");
            runtime.setHealthMessage("访问事件异步写入线程异常，统计数据可能停止落库。");
            return;
        }
        if (runtime.getBatchWriteFailures() > 0) {
            runtime.setHealthStatus("danger");
            runtime.setHealthMessage("访问事件批量写入出现失败，已降级单条写入，需要排查数据库压力。");
            return;
        }
        if (appProperties.getVisitEvent().isRocketMqMode()
                && !runtime.isRocketMqAvailable()
                && !runtime.isRocketMqFallbackToLocal()) {
            runtime.setHealthStatus("danger");
            runtime.setHealthMessage("RocketMQ 不可用且本地回退关闭，低价值统计事件会被丢弃。");
            return;
        }
        if (queueUsagePercent >= 80) {
            runtime.setHealthStatus("danger");
            runtime.setHealthMessage("访问事件本地队列接近满载，继续升高会触发丢弃。");
            return;
        }
        if (runtime.getDroppedAsyncEvents() > 0) {
            runtime.setHealthStatus("watch");
            runtime.setHealthMessage("已有访问事件被丢弃，需要关注队列容量、写入速度或 MQ 回退配置。");
            return;
        }
        if (appProperties.getVisitEvent().isRocketMqMode() && !runtime.isRocketMqAvailable()) {
            runtime.setHealthStatus("watch");
            runtime.setHealthMessage("RocketMQ 当前不可用，统计事件正在回退到本地队列。");
            return;
        }
        if (appProperties.getVisitEvent().isRocketMqMode() && runtime.isRocketMqConsumerEnabled()
                && !runtime.isRocketMqConsumerPersistenceReady()) {
            runtime.setHealthStatus("watch");
            runtime.setHealthMessage("RocketMQ 主消费开关已配置，但 consumer 落库尚未就绪，系统保持 shadow 本地落库。");
            return;
        }
        if (appProperties.getVisitEvent().isRocketMqMode() && !shouldUseRocketMqConsumerPersistence()) {
            runtime.setHealthStatus("watch");
            runtime.setHealthMessage("RocketMQ 处于 shadow 观察模式，数据中台仍由本地队列落库。");
            return;
        }
        if (appProperties.getVisitEvent().isSyncMode()) {
            runtime.setHealthStatus("ok");
            runtime.setHealthMessage("访问事件同步写入正常，适合测试和本地验收。");
            return;
        }
        runtime.setHealthStatus("ok");
        runtime.setHealthMessage("访问事件异步写入正常。");
    }

    private boolean publishToRocketMq(VisitEventEntity entity,
                                      EventType eventType,
                                      String pagePath,
                                      String resultId,
                                      String shortCode) {
        if (!rocketMqPublisher.isAvailable()) {
            recordRocketMqPublishFailure(eventType, pagePath, resultId, shortCode, "publisher unavailable");
            return false;
        }
        try {
            rocketMqPublisher.publish(entity);
            rocketMqPublishedEvents.incrementAndGet();
            return true;
        } catch (RuntimeException ex) {
            recordRocketMqPublishFailure(eventType, pagePath, resultId, shortCode,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return false;
        }
    }

    private void recordRocketMqPublishFailure(EventType eventType,
                                              String pagePath,
                                              String resultId,
                                              String shortCode,
                                              String reason) {
        long failures = rocketMqPublishFailures.incrementAndGet();
        if (failures <= 3 || failures % 100 == 0) {
            log.warn("Visit event RocketMQ publish unavailable, failures={}, eventType={}, pagePath={}, resultId={}, shortCode={}, reason={}",
                    failures, eventType, pagePath, resultId, shortCode, reason);
        }
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        asyncWorker.interrupt();
        try {
            asyncWorker.join(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        flushRemainingAsyncEvents();
    }

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

    private void flushRemainingAsyncEvents() {
        List<VisitEventEntity> batch = new ArrayList<>(asyncDrainLimit);
        asyncQueue.drainTo(batch);
        flushBatch(batch);
    }

    private void flushBatch(List<VisitEventEntity> batch) {
        if (batch.isEmpty()) {
            return;
        }
        lastBatchSize.set(batch.size());
        lastFlushAt = LocalDateTime.now();
        try {
            visitEventMapper.insertBatch(List.copyOf(batch));
            totalFlushedEvents.addAndGet(batch.size());
        } catch (RuntimeException ex) {
            batchWriteFailures.incrementAndGet();
            log.warn("Visit event batch write failed, size={}, error={}: {}", batch.size(),
                    ex.getClass().getSimpleName(), ex.getMessage());
            long degradedSuccesses = 0;
            for (VisitEventEntity entity : batch) {
                if (insertWithDegrade(entity, EventType.valueOf(entity.getEventType()), entity.getPagePath(),
                        entity.getResultId(), entity.getShortCode())) {
                    degradedSuccesses++;
                }
            }
            if (degradedSuccesses > 0) {
                totalFlushedEvents.addAndGet(degradedSuccesses);
            }
        }
    }

    private VisitEventEntity buildEntity(EventType eventType,
                                         String pagePath,
                                         String resultId,
                                         String shortCode,
                                         String clientId,
                                         HttpServletRequest request,
                                         String sessionId,
                                         String channel,
                                         String campaign) {
        VisitEventEntity entity = new VisitEventEntity();
        entity.setEventType(eventType.name());
        entity.setPagePath(truncate(pagePath, MAX_PAGE_PATH_LENGTH));
        entity.setResultId(truncate(resultId, MAX_RESULT_ID_LENGTH));
        entity.setShortCode(truncate(shortCode, MAX_SHORT_CODE_LENGTH));
        String ip = IpUtils.actualIp(request);
        String userAgent = request.getHeader("User-Agent");
        String clientHashSource = clientId;
        if (clientHashSource == null || clientHashSource.isBlank()) {
            clientHashSource = ip + "|" + userAgent;
        }
        entity.setClientIdHash(HashUtils.sha256(clientHashSource + appProperties.getHashSalt()));
        entity.setSessionIdHash(hashOrNull(firstPresent(sessionId, header(request, "X-Session-Id"),
                parameter(request, "sessionId"))));
        entity.setIpHash(HashUtils.sha256(ip + appProperties.getHashSalt()));
        entity.setUserAgentHash(HashUtils.sha256((userAgent == null ? "" : userAgent) + appProperties.getHashSalt()));
        entity.setChannel(normalizeAttribution(firstPresent(channel, header(request, "X-Channel"),
                parameter(request, "channel"), parameter(request, "utm_source"), parameter(request, "source"))));
        entity.setCampaign(normalizeAttribution(firstPresent(campaign, header(request, "X-Campaign"),
                parameter(request, "campaign"), parameter(request, "utm_campaign"))));
        entity.setDeviceType(detectDeviceType(userAgent));
        entity.setReferer(sanitizeReferer(request.getHeader("Referer")));
        LocalDateTime now = LocalDateTime.now();
        entity.setEventDate(LocalDate.now());
        entity.setCreatedAt(now);
        return entity;
    }

    private boolean insertWithDegrade(VisitEventEntity entity,
                                      EventType eventType,
                                      String pagePath,
                                      String resultId,
                                      String shortCode) {
        try {
            visitEventMapper.insert(entity);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Visit event write failed, eventType={}, pagePath={}, resultId={}, shortCode={}, error={}: {}",
                    eventType, pagePath, resultId, shortCode, ex.getClass().getSimpleName(), ex.getMessage());
            return false;
        }
    }

    private void recordSynchronously(VisitEventEntity entity,
                                     EventType eventType,
                                     String pagePath,
                                     String resultId,
                                     String shortCode) {
        if (insertWithDegrade(entity, eventType, pagePath, resultId, shortCode)) {
            totalFlushedEvents.incrementAndGet();
            lastBatchSize.set(1);
            lastFlushAt = LocalDateTime.now();
        }
    }

    private String hashOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return HashUtils.sha256(value.trim() + appProperties.getHashSalt());
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String parameter(HttpServletRequest request, String name) {
        return request == null ? null : request.getParameter(name);
    }

    private String normalizeAttribution(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        normalized = normalized.replaceAll("\\s+", "-").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() <= MAX_ATTRIBUTION_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ATTRIBUTION_LENGTH);
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("bot") || normalized.contains("crawler") || normalized.contains("spider")) {
            return "bot";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "tablet";
        }
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "mobile";
        }
        return "desktop";
    }

    private String sanitizeReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        String trimmed = referer.trim();
        try {
            URI uri = new URI(trimmed);
            String sanitized = uri.isAbsolute() ? absoluteReferer(uri) : uri.getPath();
            if (sanitized == null || sanitized.isBlank()) {
                return null;
            }
            return truncate(sanitized, MAX_REFERER_LENGTH);
        } catch (URISyntaxException ex) {
            return truncate(stripQueryAndFragment(trimmed), MAX_REFERER_LENGTH);
        }
    }

    private String absoluteReferer(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null) {
            return stripQueryAndFragment(uri.toString());
        }
        StringBuilder builder = new StringBuilder(uri.getScheme())
                .append("://")
                .append(uri.getHost());
        if (uri.getPort() >= 0) {
            builder.append(':').append(uri.getPort());
        }
        if (uri.getPath() != null && !uri.getPath().isBlank()) {
            builder.append(uri.getPath());
        }
        return builder.toString();
    }

    private String stripQueryAndFragment(String value) {
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int cutIndex = -1;
        if (queryIndex >= 0 && fragmentIndex >= 0) {
            cutIndex = Math.min(queryIndex, fragmentIndex);
        } else if (queryIndex >= 0) {
            cutIndex = queryIndex;
        } else if (fragmentIndex >= 0) {
            cutIndex = fragmentIndex;
        }
        return cutIndex >= 0 ? value.substring(0, cutIndex) : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
