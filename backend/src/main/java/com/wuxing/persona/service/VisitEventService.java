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
    private static final int ASYNC_QUEUE_CAPACITY = 2048;
    private static final int ASYNC_DRAIN_LIMIT = 64;

    private final VisitEventMapper visitEventMapper;
    private final AppProperties appProperties;
    private final BlockingQueue<VisitEventEntity> asyncQueue = new LinkedBlockingQueue<>(ASYNC_QUEUE_CAPACITY);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong droppedAsyncEvents = new AtomicLong();
    private final Thread asyncWorker;

    public VisitEventService(VisitEventMapper visitEventMapper, AppProperties appProperties) {
        this.visitEventMapper = visitEventMapper;
        this.appProperties = appProperties;
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
        if (!asyncQueue.offer(entity)) {
            long dropped = droppedAsyncEvents.incrementAndGet();
            log.warn("Visit event async queue full, dropped={}, eventType={}, pagePath={}, resultId={}, shortCode={}",
                    dropped, eventType, pagePath, resultId, shortCode);
        }
    }

    public VisitEventRuntimeVO runtime() {
        VisitEventRuntimeVO runtime = new VisitEventRuntimeVO();
        runtime.setQueueSize(asyncQueue.size());
        runtime.setQueueCapacity(ASYNC_QUEUE_CAPACITY);
        runtime.setDrainLimit(ASYNC_DRAIN_LIMIT);
        runtime.setDroppedAsyncEvents(droppedAsyncEvents.get());
        runtime.setWorkerAlive(asyncWorker.isAlive());
        return runtime;
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
        List<VisitEventEntity> batch = new ArrayList<>(ASYNC_DRAIN_LIMIT);
        while (running.get()) {
            try {
                VisitEventEntity first = asyncQueue.take();
                batch.add(first);
                asyncQueue.drainTo(batch, ASYNC_DRAIN_LIMIT - 1);
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
        List<VisitEventEntity> batch = new ArrayList<>(ASYNC_DRAIN_LIMIT);
        asyncQueue.drainTo(batch);
        flushBatch(batch);
    }

    private void flushBatch(List<VisitEventEntity> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            visitEventMapper.insertBatch(List.copyOf(batch));
        } catch (RuntimeException ex) {
            log.warn("Visit event batch write failed, size={}, error={}: {}", batch.size(),
                    ex.getClass().getSimpleName(), ex.getMessage());
            for (VisitEventEntity entity : batch) {
                insertWithDegrade(entity, EventType.valueOf(entity.getEventType()), entity.getPagePath(),
                        entity.getResultId(), entity.getShortCode());
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

    private void insertWithDegrade(VisitEventEntity entity,
                                   EventType eventType,
                                   String pagePath,
                                   String resultId,
                                   String shortCode) {
        try {
            visitEventMapper.insert(entity);
        } catch (RuntimeException ex) {
            log.warn("Visit event write failed, eventType={}, pagePath={}, resultId={}, shortCode={}, error={}: {}",
                    eventType, pagePath, resultId, shortCode, ex.getClass().getSimpleName(), ex.getMessage());
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
