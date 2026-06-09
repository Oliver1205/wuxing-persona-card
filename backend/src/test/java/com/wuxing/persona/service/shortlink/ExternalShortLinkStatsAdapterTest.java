package com.wuxing.persona.service.shortlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalShortLinkStatsAdapterTest {

    @Mock
    private ExternalShortLinkClient externalShortLinkClient;

    private AppProperties appProperties;
    private ExternalShortLinkStatsAdapter adapter;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getShortLink().setMode("external");
        appProperties.getShortLink().getExternal().setStatsEnabled(true);
        appProperties.getShortLink().getExternal().setDomain("nurl.ink:8003");
        appProperties.getShortLink().getExternal().setGroupId("wuxing_persona");
        adapter = new ExternalShortLinkStatsAdapter(appProperties, externalShortLinkClient);
    }

    @Test
    void shouldFetchExternalStatsWhenEnabledAndDomainMatches() {
        ExternalShortLinkStatsResponse response = new ExternalShortLinkStatsResponse();
        response.setPv(9L);
        response.setUv(4L);
        response.setUip(3L);
        when(externalShortLinkClient.stats(any(ExternalShortLinkStatsRequest.class))).thenReturn(response);

        Optional<ExternalShortLinkStatsSnapshot> snapshot = adapter.fetchStats(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(snapshot.isPresent());
        assertEquals(9L, snapshot.get().getPv());
        assertEquals(4L, snapshot.get().getUv());
        assertEquals(3L, snapshot.get().getUip());
        ArgumentCaptor<ExternalShortLinkStatsRequest> requestCaptor =
                ArgumentCaptor.forClass(ExternalShortLinkStatsRequest.class);
        verify(externalShortLinkClient).stats(requestCaptor.capture());
        ExternalShortLinkStatsRequest request = requestCaptor.getValue();
        assertEquals("nurl.ink:8003/abc123", request.getFullShortUrl());
        assertEquals("wuxing_persona", request.getGid());
        assertEquals(0, request.getEnableStatus());
        assertEquals("2026-06-08", request.getStartDate());
        assertEquals("2026-06-10", request.getEndDate());
    }

    @Test
    void shouldFallbackToLocalStatsWhenExternalStatsFails() {
        when(externalShortLinkClient.stats(any(ExternalShortLinkStatsRequest.class)))
                .thenThrow(new BusinessException("external short link stats unavailable"));

        Optional<ExternalShortLinkStatsSnapshot> snapshot = adapter.fetchStats(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void shouldSkipExternalStatsWhenModeIsInternal() {
        appProperties.getShortLink().setMode("internal");

        Optional<ExternalShortLinkStatsSnapshot> snapshot = adapter.fetchStats(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(snapshot.isEmpty());
        verify(externalShortLinkClient, never()).stats(any());
    }

    @Test
    void shouldSkipExternalStatsWhenDomainDoesNotMatch() {
        Optional<ExternalShortLinkStatsSnapshot> snapshot = adapter.fetchStats(
                shortLink("abc123", "http://localhost:8080/s/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(snapshot.isEmpty());
        verify(externalShortLinkClient, never()).stats(any());
    }

    private ShortLinkEntity shortLink(String shortCode, String shortUrl) {
        ShortLinkEntity entity = new ShortLinkEntity();
        entity.setShortCode(shortCode);
        entity.setShortUrl(shortUrl);
        entity.setCreatedAt(LocalDateTime.of(2026, 6, 8, 12, 0));
        return entity;
    }
}
