package com.wuxing.persona.service;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.dto.AnalyticsEventRequest;
import com.wuxing.persona.dto.AnalyticsSessionRequest;
import com.wuxing.persona.entity.AnalyticsMetricSnapshotEntity;
import com.wuxing.persona.entity.AnalyticsSessionEntity;
import com.wuxing.persona.entity.AnalyticsVisitorEntity;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.AnalyticsMetricSnapshotMapper;
import com.wuxing.persona.mapper.AnalyticsSessionMapper;
import com.wuxing.persona.mapper.AnalyticsVisitorMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.util.IpUtils;
import com.wuxing.persona.vo.MetricTimeseriesPointVO;
import com.wuxing.persona.vo.MetricTimeseriesVO;
import com.wuxing.persona.vo.RealtimeMetricsVO;
import com.wuxing.persona.vo.RecentMetricEventVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRealtimeService.class);
    private static final int MAX_PATH_LENGTH = 255;
    private static final int MAX_REFERER_LENGTH = 512;
    private static final int SNAPSHOT_WRITE_ATTEMPTS = 3;
    private static final List<String> SHARE_EVENTS = List.of(
            EventType.SAVE_SHARE_IMAGE_SUCCESS.name(),
            EventType.NATIVE_SHARE_SUCCESS.name(),
            EventType.SHORT_LINK_COPY.name(),
            EventType.COPY_LINK.name(),
            EventType.SHARE_CLICK.name()
    );
    private static final List<String> MATCH_EVENTS = List.of(
            EventType.MATCH_SHORT_CODE_ENTERED.name(),
            EventType.MATCH_MODE_ACCEPT.name(),
            EventType.MATCH_ENTER.name()
    );
    private static final List<String> RESULT_EVENTS = List.of(
            EventType.RESULT_CREATED.name(),
            EventType.RESULT_GENERATED.name()
    );

    private final AnalyticsVisitorMapper analyticsVisitorMapper;
    private final AnalyticsSessionMapper analyticsSessionMapper;
    private final AnalyticsMetricSnapshotMapper metricSnapshotMapper;
    private final VisitEventMapper visitEventMapper;
    private final VisitEventService visitEventService;
    private final AppProperties appProperties;

    public AnalyticsRealtimeService(AnalyticsVisitorMapper analyticsVisitorMapper,
                                    AnalyticsSessionMapper analyticsSessionMapper,
                                    AnalyticsMetricSnapshotMapper metricSnapshotMapper,
                                    VisitEventMapper visitEventMapper,
                                    VisitEventService visitEventService,
                                    AppProperties appProperties) {
        this.analyticsVisitorMapper = analyticsVisitorMapper;
        this.analyticsSessionMapper = analyticsSessionMapper;
        this.metricSnapshotMapper = metricSnapshotMapper;
        this.visitEventMapper = visitEventMapper;
        this.visitEventService = visitEventService;
        this.appProperties = appProperties;
    }

    @Transactional
    public void startSession(AnalyticsSessionRequest request,
                             String clientIdHeader,
                             String sessionIdHeader,
                             HttpServletRequest servletRequest) {
        if (!appProperties.getAnalytics().isEnabled()) {
            return;
        }
        touchSession(request, clientIdHeader, sessionIdHeader, servletRequest, true, false);
        recordMappedEvent(EventType.SESSION_START, path(request), null, null, clientIdHeader, sessionIdHeader,
                servletRequest);
    }

    @Transactional
    public void heartbeat(AnalyticsSessionRequest request,
                          String clientIdHeader,
                          String sessionIdHeader,
                          HttpServletRequest servletRequest) {
        if (!appProperties.getAnalytics().isEnabled()) {
            return;
        }
        touchSession(request, clientIdHeader, sessionIdHeader, servletRequest, false, false);
        snapshotCurrentMinute();
    }

    @Transactional
    public void endSession(AnalyticsSessionRequest request,
                           String clientIdHeader,
                           String sessionIdHeader,
                           HttpServletRequest servletRequest) {
        if (!appProperties.getAnalytics().isEnabled()) {
            return;
        }
        touchSession(request, clientIdHeader, sessionIdHeader, servletRequest, false, true);
        recordMappedEvent(EventType.SESSION_END, path(request), null, null, clientIdHeader, sessionIdHeader,
                servletRequest);
        snapshotCurrentMinute();
    }

    public void recordEvent(AnalyticsEventRequest request,
                            String clientIdHeader,
                            String sessionIdHeader,
                            HttpServletRequest servletRequest) {
        if (!appProperties.getAnalytics().isEnabled()) {
            return;
        }
        EventType eventType = mapEventName(request.getEventName());
        if (eventType == null) {
            return;
        }
        recordMappedEvent(eventType, firstPresent(request.getPath(), "/"), request.getResultId(), request.getShortCode(),
                firstPresent(request.getVisitorId(), clientIdHeader), firstPresent(request.getSessionId(), sessionIdHeader),
                servletRequest);
    }

    @Transactional
    public RealtimeMetricsVO realtime() {
        snapshotCurrentMinute();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        RealtimeMetricsVO vo = new RealtimeMetricsVO();
        vo.setCurrentOnlineVisitors(analyticsSessionMapper.countOnlineVisitors(onlineCutoff(now)));
        vo.setCurrentOnlineSessions(analyticsSessionMapper.countOnlineSessions(onlineCutoff(now)));
        vo.setTodayPv(visitEventMapper.countAllBetween(startOfDay, now.plusSeconds(1)));
        vo.setTodayUv(visitEventMapper.countDistinctClientBetween(startOfDay, now.plusSeconds(1)));
        vo.setTodayResults(visitEventMapper.countByEventTypesBetween(RESULT_EVENTS, startOfDay, now.plusSeconds(1)));
        vo.setTodayShareClicks(visitEventMapper.countByEventTypesBetween(SHARE_EVENTS, startOfDay, now.plusSeconds(1)));
        vo.setTodayMatchEnters(visitEventMapper.countByEventTypesBetween(MATCH_EVENTS, startOfDay, now.plusSeconds(1)));
        vo.setHeartbeatIntervalSeconds(appProperties.getAnalytics().getHeartbeatIntervalMillis() / 1000);
        vo.setOnlineWindowSeconds(appProperties.getAnalytics().getOnlineWindowMillis() / 1000);
        vo.setRefreshedAt(now.truncatedTo(ChronoUnit.SECONDS).toString());
        return vo;
    }

    @Transactional
    public MetricTimeseriesVO timeseries(String range) {
        snapshotCurrentMinute();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        RangeWindow window = RangeWindow.of(range, now);
        List<AnalyticsMetricSnapshotEntity> raw = metricSnapshotMapper.listBetween(window.startAt(), now.plusMinutes(1));
        MetricTimeseriesVO vo = new MetricTimeseriesVO();
        vo.setRange(window.range());
        vo.setIntervalSeconds((int) window.bucket().getSeconds());
        vo.setGeneratedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        vo.setPoints(bucket(raw, window));
        return vo;
    }

    public List<RecentMetricEventVO> recentEvents(String range) {
        LocalDateTime now = LocalDateTime.now();
        RangeWindow window = RangeWindow.of(range, now);
        return visitEventMapper.listRecentBetween(window.startAt(), now.plusSeconds(1), 20).stream()
                .map(this::toRecentEvent)
                .toList();
    }

    private void touchSession(AnalyticsSessionRequest request,
                              String clientIdHeader,
                              String sessionIdHeader,
                              HttpServletRequest servletRequest,
                              boolean sessionStart,
                              boolean sessionEnd) {
        LocalDateTime now = LocalDateTime.now();
        String visitorId = firstPresent(request == null ? null : request.getVisitorId(), clientIdHeader);
        String sessionId = firstPresent(request == null ? null : request.getSessionId(), sessionIdHeader);
        String visitorHash = hashRequired(visitorId, "visitorId");
        String sessionHash = hashRequired(sessionId, "sessionId");
        String ip = IpUtils.actualIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        AnalyticsVisitorEntity visitor = new AnalyticsVisitorEntity();
        visitor.setVisitorIdHash(visitorHash);
        visitor.setFirstSeenAt(now);
        visitor.setLastSeenAt(now);
        visitor.setIpHash(HashUtils.sha256(ip + appProperties.getHashSalt()));
        visitor.setUserAgentHash(HashUtils.sha256((userAgent == null ? "" : userAgent) + appProperties.getHashSalt()));
        visitor.setCreatedAt(now);
        visitor.setUpdatedAt(now);
        touchVisitor(visitor);

        AnalyticsSessionEntity session = new AnalyticsSessionEntity();
        session.setSessionIdHash(sessionHash);
        session.setVisitorIdHash(visitorHash);
        session.setStartedAt(now);
        session.setLastHeartbeatAt(now);
        session.setEndedAt(sessionEnd ? now : null);
        session.setEntryPath(path(request));
        session.setLatestPath(path(request));
        session.setReferrer(truncate(servletRequest.getHeader("Referer"), MAX_REFERER_LENGTH));
        session.setDeviceType(detectDeviceType(userAgent));
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        if (analyticsSessionMapper.selectBySessionIdHash(sessionHash) == null) {
            try {
                analyticsSessionMapper.insert(session);
                return;
            } catch (DuplicateKeyException ignored) {
                // Another heartbeat/start request may have created the same session first.
                // Fall through to update the existing row and keep analytics write idempotent.
            }
        }
        if (sessionEnd) {
            analyticsSessionMapper.endSession(session);
            return;
        }
        analyticsSessionMapper.updateHeartbeat(session);
    }

    private void snapshotCurrentMinute() {
        for (int attempt = 1; attempt <= SNAPSHOT_WRITE_ATTEMPTS; attempt++) {
            try {
                writeSnapshotCurrentMinute();
                return;
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException ex) {
                if (attempt == SNAPSHOT_WRITE_ATTEMPTS) {
                    log.warn("Skip realtime metric snapshot after {} attempts: {}", attempt, ex.getMessage());
                    return;
                }
                sleepBeforeSnapshotRetry(attempt);
            }
        }
    }

    private void writeSnapshotCurrentMinute() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime metricTime = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime minuteEnd = metricTime.plusMinutes(1);
        AnalyticsMetricSnapshotEntity snapshot = new AnalyticsMetricSnapshotEntity();
        snapshot.setMetricTime(metricTime);
        snapshot.setOnlineVisitors(analyticsSessionMapper.countOnlineVisitors(onlineCutoff(now)));
        snapshot.setOnlineSessions(analyticsSessionMapper.countOnlineSessions(onlineCutoff(now)));
        snapshot.setPv1m(visitEventMapper.countAllBetween(metricTime, minuteEnd));
        snapshot.setUv1m(visitEventMapper.countDistinctClientBetween(metricTime, minuteEnd));
        snapshot.setResultGenerated1m(visitEventMapper.countByEventTypesBetween(RESULT_EVENTS, metricTime, minuteEnd));
        snapshot.setShareClick1m(visitEventMapper.countByEventTypesBetween(SHARE_EVENTS, metricTime, minuteEnd));
        snapshot.setMatchEnter1m(visitEventMapper.countByEventTypesBetween(MATCH_EVENTS, metricTime, minuteEnd));
        snapshot.setCreatedAt(now);
        if (metricSnapshotMapper.selectByMetricTime(metricTime) == null) {
            try {
                metricSnapshotMapper.insert(snapshot);
            } catch (DuplicateKeyException ignored) {
                metricSnapshotMapper.update(snapshot);
            }
        } else {
            metricSnapshotMapper.update(snapshot);
        }
    }

    private void sleepBeforeSnapshotRetry(int attempt) {
        try {
            Thread.sleep(attempt * 25L);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void touchVisitor(AnalyticsVisitorEntity visitor) {
        if (analyticsVisitorMapper.selectByVisitorIdHash(visitor.getVisitorIdHash()) == null) {
            try {
                analyticsVisitorMapper.insert(visitor);
                return;
            } catch (DuplicateKeyException ignored) {
                // Visitor creation is keyed by an anonymous hash. Concurrent page load,
                // heartbeat and event requests can legitimately race here.
            }
        }
        analyticsVisitorMapper.updateLastSeen(visitor);
    }

    private List<MetricTimeseriesPointVO> bucket(List<AnalyticsMetricSnapshotEntity> raw, RangeWindow window) {
        Map<LocalDateTime, MetricTimeseriesPointVO> buckets = new LinkedHashMap<>();
        LocalDateTime cursor = alignBucket(window.startAt(), window.bucket());
        while (!cursor.isAfter(window.endAt())) {
            buckets.put(cursor, emptyPoint(cursor));
            cursor = cursor.plus(window.bucket());
        }
        for (AnalyticsMetricSnapshotEntity row : raw) {
            LocalDateTime bucketTime = alignBucket(row.getMetricTime(), window.bucket());
            MetricTimeseriesPointVO point = buckets.computeIfAbsent(bucketTime, this::emptyPoint);
            point.setOnlineVisitors(Math.max(point.getOnlineVisitors(), row.getOnlineVisitors()));
            point.setOnlineSessions(Math.max(point.getOnlineSessions(), row.getOnlineSessions()));
            point.setPv(point.getPv() + row.getPv1m());
            point.setUv(Math.max(point.getUv(), row.getUv1m()));
            point.setResultGenerated(point.getResultGenerated() + row.getResultGenerated1m());
            point.setShareClicks(point.getShareClicks() + row.getShareClick1m());
            point.setMatchEnters(point.getMatchEnters() + row.getMatchEnter1m());
        }
        return new ArrayList<>(buckets.values());
    }

    private MetricTimeseriesPointVO emptyPoint(LocalDateTime time) {
        MetricTimeseriesPointVO point = new MetricTimeseriesPointVO();
        point.setTime(time.toString());
        return point;
    }

    private LocalDateTime alignBucket(LocalDateTime time, Duration bucket) {
        long seconds = bucket.getSeconds();
        long epochMinute = time.truncatedTo(ChronoUnit.MINUTES).toEpochSecond(java.time.ZoneOffset.UTC) / seconds;
        return LocalDateTime.ofEpochSecond(epochMinute * seconds, 0, java.time.ZoneOffset.UTC);
    }

    private RecentMetricEventVO toRecentEvent(VisitEventEntity entity) {
        RecentMetricEventVO vo = new RecentMetricEventVO();
        vo.setEventType(entity.getEventType());
        vo.setPagePath(entity.getPagePath());
        vo.setDeviceType(entity.getDeviceType());
        vo.setChannel(entity.getChannel());
        vo.setCampaign(entity.getCampaign());
        vo.setOccurredAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        return vo;
    }

    private EventType mapEventName(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return null;
        }
        return switch (eventName.trim().toLowerCase(Locale.ROOT)) {
            case "session_start" -> EventType.SESSION_START;
            case "session_end" -> EventType.SESSION_END;
            case "page_view" -> EventType.PAGE_VIEW;
            case "heartbeat" -> null;
            case "result_generated" -> EventType.RESULT_GENERATED;
            case "share_image_click" -> EventType.SAVE_SHARE_IMAGE_SUCCESS;
            case "copy_match_code" -> EventType.SHORT_LINK_COPY;
            case "copy_link" -> EventType.COPY_LINK;
            case "share_click" -> EventType.SHARE_CLICK;
            case "match_enter" -> EventType.MATCH_ENTER;
            default -> {
                try {
                    yield EventType.fromCode(eventName);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }

    private void recordMappedEvent(EventType eventType,
                                   String pagePath,
                                   String resultId,
                                   String shortCode,
                                   String visitorId,
                                   String sessionId,
                                   HttpServletRequest request) {
        visitEventService.recordAsync(eventType, truncate(pagePath, MAX_PATH_LENGTH), resultId, shortCode,
                visitorId, request, sessionId, null, null);
    }

    private LocalDateTime onlineCutoff(LocalDateTime now) {
        return now.minus(appProperties.getAnalytics().getOnlineWindowMillis(), ChronoUnit.MILLIS);
    }

    private String path(AnalyticsSessionRequest request) {
        return truncate(firstPresent(request == null ? null : request.getPath(), "/"), MAX_PATH_LENGTH);
    }

    private String hashRequired(String value, String fieldName) {
        String hash = HashUtils.sha256(firstPresent(value, "missing") + appProperties.getHashSalt());
        if (hash == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return hash;
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("mobile") || normalized.contains("iphone") || normalized.contains("android")) {
            return "mobile";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "tablet";
        }
        return "desktop";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String firstPresent(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record RangeWindow(String range, LocalDateTime startAt, LocalDateTime endAt, Duration bucket) {

        static RangeWindow of(String range, LocalDateTime now) {
            String normalized = range == null || range.isBlank() ? "1h" : range.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "24h" -> new RangeWindow("24h", now.minusHours(24), now, Duration.ofMinutes(5));
                case "7d" -> new RangeWindow("7d", now.minusDays(7), now, Duration.ofHours(1));
                case "30d" -> new RangeWindow("30d", now.minusDays(30), now, Duration.ofHours(6));
                default -> new RangeWindow("1h", now.minusHours(1), now, Duration.ofMinutes(1));
            };
        }
    }
}
