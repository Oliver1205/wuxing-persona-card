package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExternalShortLinkStatsAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalShortLinkStatsAdapter.class);

    private final AppProperties appProperties;
    private final ExternalShortLinkClient externalShortLinkClient;
    private final ConcurrentMap<String, CachedStats> statsCache = new ConcurrentHashMap<>();

    public ExternalShortLinkStatsAdapter(AppProperties appProperties, ExternalShortLinkClient externalShortLinkClient) {
        this.appProperties = appProperties;
        this.externalShortLinkClient = externalShortLinkClient;
    }

    public Optional<ExternalShortLinkStatsSnapshot> fetchStats(ShortLinkEntity shortLink, AdminDateRange range) {
        return fetchStats(shortLink, range, false);
    }

    public Optional<ExternalShortLinkStatsSnapshot> fetchStatsStrict(ShortLinkEntity shortLink, AdminDateRange range) {
        return fetchStats(shortLink, range, true);
    }

    private Optional<ExternalShortLinkStatsSnapshot> fetchStats(ShortLinkEntity shortLink,
                                                               AdminDateRange range,
                                                               boolean strict) {
        AppProperties.ExternalShortLinkProperties external = appProperties.getShortLink().getExternal();
        if (!isExternalStatsEnabled(external)) {
            if (strict) {
                throw new BusinessException(502, "external short link stats unavailable: disabled");
            }
            return Optional.empty();
        }
        String fullShortUrl = toExternalFullShortUrl(shortLink.getShortUrl(), external.getDomain());
        if (fullShortUrl == null) {
            if (strict) {
                throw new BusinessException(502, "external short link stats unavailable: domain mismatch");
            }
            return Optional.empty();
        }
        try {
            ExternalShortLinkStatsRequest request = buildRequest(shortLink, range, external, fullShortUrl);
            Optional<ExternalShortLinkStatsSnapshot> cached = readStatsCache(request, external);
            if (cached.isPresent()) {
                return cached;
            }
            ExternalShortLinkStatsResponse response = externalShortLinkClient.stats(request);
            ExternalShortLinkStatsSnapshot snapshot = new ExternalShortLinkStatsSnapshot(
                    nullToZero(response.getPv()),
                    nullToZero(response.getUv()),
                    nullToZero(response.getUip())
            );
            writeStatsCache(request, external, snapshot);
            return Optional.of(snapshot);
        } catch (BusinessException ex) {
            log.warn("External short link stats unavailable, fallback to local stats, shortCode={}", shortLink.getShortCode());
            if (strict) {
                throw new BusinessException(502, "external short link stats unavailable");
            }
            return Optional.empty();
        }
    }

    public Optional<PageVO<ShortLinkVisitVO>> fetchAccessRecords(ShortLinkEntity shortLink,
                                                                 long page,
                                                                 long pageSize,
                                                                 AdminDateRange range) {
        AppProperties.ExternalShortLinkProperties external = appProperties.getShortLink().getExternal();
        if (!isExternalStatsEnabled(external)) {
            return Optional.empty();
        }
        String fullShortUrl = toExternalFullShortUrl(shortLink.getShortUrl(), external.getDomain());
        if (fullShortUrl == null) {
            return Optional.empty();
        }
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        try {
            ExternalShortLinkAccessRecordRequest request = buildAccessRecordRequest(shortLink, range, external,
                    fullShortUrl, normalizedPage, normalizedPageSize);
            ExternalShortLinkAccessRecordPageResponse response = externalShortLinkClient.accessRecords(request);
            return Optional.of(new PageVO<>(
                    firstNonNull(response.getCurrent(), normalizedPage),
                    firstNonNull(response.getSize(), normalizedPageSize),
                    nullToZero(response.getTotal()),
                    recordsOrEmpty(response).stream()
                            .map(this::toExternalVisit)
                            .toList()
            ));
        } catch (BusinessException ex) {
            log.warn("External short link access records unavailable, fallback to local records, shortCode={}",
                    shortLink.getShortCode());
            return Optional.empty();
        }
    }

    private ExternalShortLinkStatsRequest buildRequest(ShortLinkEntity shortLink,
                                                       AdminDateRange range,
                                                       AppProperties.ExternalShortLinkProperties external,
                                                       String fullShortUrl) {
        LocalDate startDate = range.getStartDate();
        if (startDate == null) {
            startDate = shortLink.getCreatedAt() == null ? LocalDate.now() : shortLink.getCreatedAt().toLocalDate();
        }
        LocalDate endDate = range.getEndDate() == null ? LocalDate.now() : range.getEndDate();
        if (endDate.isBefore(startDate)) {
            startDate = endDate;
        }
        ExternalShortLinkStatsRequest request = new ExternalShortLinkStatsRequest();
        request.setFullShortUrl(fullShortUrl);
        request.setGid(external.getGroupId());
        request.setEnableStatus(external.getStatsEnableStatus());
        request.setStartDate(startDate.toString());
        // The external shortlink service uses endDate as a timestamp boundary in access-log queries.
        request.setEndDate(endDate.plusDays(1).toString());
        return request;
    }

    private Optional<ExternalShortLinkStatsSnapshot> readStatsCache(ExternalShortLinkStatsRequest request,
                                                                    AppProperties.ExternalShortLinkProperties external) {
        long ttlSeconds = external.getStatsCacheTtlSeconds();
        if (ttlSeconds <= 0) {
            return Optional.empty();
        }
        String key = statsCacheKey(request);
        CachedStats cached = statsCache.get(key);
        if (cached == null) {
            return Optional.empty();
        }
        if (Duration.between(cached.cachedAt(), LocalDateTime.now()).getSeconds() >= ttlSeconds) {
            statsCache.remove(key, cached);
            return Optional.empty();
        }
        return Optional.of(cached.snapshot());
    }

    private void writeStatsCache(ExternalShortLinkStatsRequest request,
                                 AppProperties.ExternalShortLinkProperties external,
                                 ExternalShortLinkStatsSnapshot snapshot) {
        if (external.getStatsCacheTtlSeconds() <= 0) {
            return;
        }
        statsCache.put(statsCacheKey(request), new CachedStats(snapshot, LocalDateTime.now()));
    }

    private String statsCacheKey(ExternalShortLinkStatsRequest request) {
        return String.join("|",
                request.getFullShortUrl(),
                request.getGid(),
                String.valueOf(request.getEnableStatus()),
                request.getStartDate(),
                request.getEndDate());
    }

    private ExternalShortLinkAccessRecordRequest buildAccessRecordRequest(ShortLinkEntity shortLink,
                                                                          AdminDateRange range,
                                                                          AppProperties.ExternalShortLinkProperties external,
                                                                          String fullShortUrl,
                                                                          long page,
                                                                          long pageSize) {
        ExternalShortLinkStatsRequest statsRequest = buildRequest(shortLink, range, external, fullShortUrl);
        ExternalShortLinkAccessRecordRequest request = new ExternalShortLinkAccessRecordRequest();
        request.setFullShortUrl(statsRequest.getFullShortUrl());
        request.setGid(statsRequest.getGid());
        request.setEnableStatus(statsRequest.getEnableStatus());
        request.setStartDate(statsRequest.getStartDate());
        request.setEndDate(statsRequest.getEndDate());
        request.setCurrent(page);
        request.setSize(pageSize);
        return request;
    }

    private ShortLinkVisitVO toExternalVisit(ExternalShortLinkAccessRecordResponse record) {
        ShortLinkVisitVO vo = new ShortLinkVisitVO();
        vo.setCreatedAt(record.getCreateTime());
        vo.setEventType("EXTERNAL_SHORT_LINK_VISIT");
        vo.setClientIdHash(saltedHash(record.getUser()));
        vo.setIpHash(saltedHash(record.getIp()));
        vo.setUserAgentHash(saltedHash(fingerprint(record)));
        vo.setReferer(describeExternalRecord(record));
        vo.setStatSource("external");
        return vo;
    }

    private boolean isExternalStatsEnabled(AppProperties.ExternalShortLinkProperties external) {
        return "external".equalsIgnoreCase(appProperties.getShortLink().getMode()) && external.isStatsEnabled();
    }

    private String saltedHash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return HashUtils.sha256(value + appProperties.getHashSalt());
    }

    private String fingerprint(ExternalShortLinkAccessRecordResponse record) {
        return joinNonBlank(record.getBrowser(), record.getOs(), record.getNetwork(), record.getDevice(), record.getLocale());
    }

    private String describeExternalRecord(ExternalShortLinkAccessRecordResponse record) {
        String detail = joinNonBlank(
                label("uvType", record.getUvType()),
                label("browser", record.getBrowser()),
                label("os", record.getOs()),
                label("network", record.getNetwork()),
                label("device", record.getDevice()),
                label("locale", record.getLocale())
        );
        return detail.isBlank() ? null : detail;
    }

    private String label(String label, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return label + "=" + value.trim();
    }

    private String joinNonBlank(String... values) {
        java.util.List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value.trim());
            }
        }
        return String.join("; ", parts);
    }

    private List<ExternalShortLinkAccessRecordResponse> recordsOrEmpty(
            ExternalShortLinkAccessRecordPageResponse response) {
        return response.getRecords() == null ? List.of() : response.getRecords();
    }

    private String toExternalFullShortUrl(String shortUrl, String configuredDomain) {
        if (shortUrl == null || shortUrl.isBlank()) {
            return null;
        }
        String value = stripSchemeAndSuffix(shortUrl);
        String domain = stripSchemeAndSuffix(configuredDomain);
        if (domain != null && !value.startsWith(domain + "/")) {
            return null;
        }
        return value;
    }

    private String stripSchemeAndSuffix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.trim();
        int schemeIndex = result.indexOf("://");
        if (schemeIndex >= 0) {
            result = result.substring(schemeIndex + 3);
        }
        int queryIndex = result.indexOf('?');
        if (queryIndex >= 0) {
            result = result.substring(0, queryIndex);
        }
        int fragmentIndex = result.indexOf('#');
        if (fragmentIndex >= 0) {
            result = result.substring(0, fragmentIndex);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private long firstNonNull(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private record CachedStats(ExternalShortLinkStatsSnapshot snapshot, LocalDateTime cachedAt) {
    }
}
