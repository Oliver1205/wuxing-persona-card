package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.vo.ExternalShortLinkRuntimeVO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ExternalShortLinkRuntimeService {

    private final AppProperties appProperties;

    public ExternalShortLinkRuntimeService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public ExternalShortLinkRuntimeVO status(boolean probe) {
        AppProperties.ShortLinkProperties shortLink = appProperties.getShortLink();
        AppProperties.ExternalShortLinkProperties external = shortLink.getExternal();
        ExternalShortLinkRuntimeVO vo = new ExternalShortLinkRuntimeVO();
        vo.setMode(shortLink.getMode());
        vo.setExternalMode("external".equalsIgnoreCase(shortLink.getMode()));
        vo.setStatsEnabled(external.isStatsEnabled());
        vo.setFallbackToInternal(external.isFallbackToInternal());
        vo.setBaseUrl(external.getBaseUrl());
        vo.setDomain(external.getDomain());
        vo.setGroupId(external.getGroupId());
        vo.setCheckedAt(LocalDateTime.now());
        if (!probe) {
            vo.setMessage("probe skipped");
            return vo;
        }
        probeExternalService(external, vo);
        return vo;
    }

    private void probeExternalService(AppProperties.ExternalShortLinkProperties external,
                                      ExternalShortLinkRuntimeVO vo) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(external.getConnectTimeoutMillis()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(external.getBaseUrl()))
                    .timeout(Duration.ofMillis(external.getReadTimeoutMillis()))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            vo.setHttpStatus(response.statusCode());
            vo.setReachable(response.statusCode() > 0 && response.statusCode() < 500);
            vo.setMessage("probe completed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            vo.setReachable(false);
            vo.setMessage("probe interrupted");
        } catch (RuntimeException | java.io.IOException ex) {
            vo.setReachable(false);
            vo.setMessage("probe failed: " + ex.getClass().getSimpleName());
        }
    }
}
