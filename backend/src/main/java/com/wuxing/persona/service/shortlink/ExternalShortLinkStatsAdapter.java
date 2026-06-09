package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExternalShortLinkStatsAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalShortLinkStatsAdapter.class);

    private final AppProperties appProperties;
    private final ExternalShortLinkClient externalShortLinkClient;

    public ExternalShortLinkStatsAdapter(AppProperties appProperties, ExternalShortLinkClient externalShortLinkClient) {
        this.appProperties = appProperties;
        this.externalShortLinkClient = externalShortLinkClient;
    }

    public Optional<ExternalShortLinkStatsSnapshot> fetchStats(ShortLinkEntity shortLink, AdminDateRange range) {
        AppProperties.ExternalShortLinkProperties external = appProperties.getShortLink().getExternal();
        if (!isExternalStatsEnabled(external)) {
            return Optional.empty();
        }
        String fullShortUrl = toExternalFullShortUrl(shortLink.getShortUrl(), external.getDomain());
        if (fullShortUrl == null) {
            return Optional.empty();
        }
        try {
            ExternalShortLinkStatsRequest request = buildRequest(shortLink, range, external, fullShortUrl);
            ExternalShortLinkStatsResponse response = externalShortLinkClient.stats(request);
            return Optional.of(new ExternalShortLinkStatsSnapshot(
                    nullToZero(response.getPv()),
                    nullToZero(response.getUv()),
                    nullToZero(response.getUip())
            ));
        } catch (BusinessException ex) {
            log.warn("External short link stats unavailable, fallback to local stats, shortCode={}", shortLink.getShortCode());
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
                    response.getRecords().stream()
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
}
