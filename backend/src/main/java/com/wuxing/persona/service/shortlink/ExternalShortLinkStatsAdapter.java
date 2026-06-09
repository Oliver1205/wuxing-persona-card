package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import java.time.LocalDate;
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
        if (!"external".equalsIgnoreCase(appProperties.getShortLink().getMode()) || !external.isStatsEnabled()) {
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
}
