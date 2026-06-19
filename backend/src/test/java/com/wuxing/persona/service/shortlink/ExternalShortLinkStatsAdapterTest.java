package com.wuxing.persona.service.shortlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        appProperties.setHashSalt("test-salt");
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
    void shouldCacheExternalStatsWithinTtlForSameRange() {
        ExternalShortLinkStatsResponse response = new ExternalShortLinkStatsResponse();
        response.setPv(9L);
        response.setUv(4L);
        response.setUip(3L);
        when(externalShortLinkClient.stats(any(ExternalShortLinkStatsRequest.class))).thenReturn(response);
        ShortLinkEntity shortLink = shortLink("abc123", "http://nurl.ink:8003/abc123");
        AdminDateRange range = AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9));

        Optional<ExternalShortLinkStatsSnapshot> first = adapter.fetchStats(shortLink, range);
        Optional<ExternalShortLinkStatsSnapshot> second = adapter.fetchStats(shortLink, range);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(9L, second.get().getPv());
        verify(externalShortLinkClient, times(1)).stats(any(ExternalShortLinkStatsRequest.class));
    }

    @Test
    void shouldBypassExternalStatsCacheWhenTtlIsZero() {
        appProperties.getShortLink().getExternal().setStatsCacheTtlSeconds(0);
        ExternalShortLinkStatsResponse response = new ExternalShortLinkStatsResponse();
        response.setPv(9L);
        response.setUv(4L);
        response.setUip(3L);
        when(externalShortLinkClient.stats(any(ExternalShortLinkStatsRequest.class))).thenReturn(response);
        ShortLinkEntity shortLink = shortLink("abc123", "http://nurl.ink:8003/abc123");
        AdminDateRange range = AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9));

        adapter.fetchStats(shortLink, range);
        adapter.fetchStats(shortLink, range);

        verify(externalShortLinkClient, times(2)).stats(any(ExternalShortLinkStatsRequest.class));
    }

    @Test
    void shouldFetchExternalAccessRecordsWhenEnabledAndDomainMatches() {
        ExternalShortLinkAccessRecordResponse record = new ExternalShortLinkAccessRecordResponse();
        record.setUvType("新访客");
        record.setBrowser("Chrome");
        record.setOs("macOS");
        record.setIp("127.0.0.1");
        record.setNetwork("WiFi");
        record.setDevice("PC");
        record.setLocale("上海");
        record.setUser("uv-1");
        record.setCreateTime(LocalDateTime.of(2026, 6, 9, 15, 30));
        ExternalShortLinkAccessRecordPageResponse response = new ExternalShortLinkAccessRecordPageResponse();
        response.setCurrent(2L);
        response.setSize(10L);
        response.setTotal(1L);
        response.setRecords(List.of(record));
        when(externalShortLinkClient.accessRecords(any(ExternalShortLinkAccessRecordRequest.class))).thenReturn(response);

        Optional<PageVO<ShortLinkVisitVO>> page = adapter.fetchAccessRecords(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                2,
                10,
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(page.isPresent());
        assertEquals(2L, page.get().getPage());
        assertEquals(10L, page.get().getPageSize());
        assertEquals(1L, page.get().getTotal());
        ShortLinkVisitVO visit = page.get().getRecords().get(0);
        assertEquals(LocalDateTime.of(2026, 6, 9, 15, 30), visit.getCreatedAt());
        assertEquals("EXTERNAL_SHORT_LINK_VISIT", visit.getEventType());
        assertEquals("external", visit.getStatSource());
        assertEquals(HashUtils.sha256("uv-1test-salt"), visit.getClientIdHash());
        assertEquals(HashUtils.sha256("127.0.0.1test-salt"), visit.getIpHash());
        assertTrue(visit.getReferer().contains("browser=Chrome"));
        assertTrue(visit.getReferer().contains("locale=上海"));
        ArgumentCaptor<ExternalShortLinkAccessRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(ExternalShortLinkAccessRecordRequest.class);
        verify(externalShortLinkClient).accessRecords(requestCaptor.capture());
        ExternalShortLinkAccessRecordRequest request = requestCaptor.getValue();
        assertEquals("nurl.ink:8003/abc123", request.getFullShortUrl());
        assertEquals("wuxing_persona", request.getGid());
        assertEquals(0, request.getEnableStatus());
        assertEquals("2026-06-08", request.getStartDate());
        assertEquals("2026-06-10", request.getEndDate());
        assertEquals(2L, request.getCurrent());
        assertEquals(10L, request.getSize());
    }

    @Test
    void shouldReturnEmptyAccessRecordsWhenExternalRecordsAreNull() {
        ExternalShortLinkAccessRecordPageResponse response = new ExternalShortLinkAccessRecordPageResponse();
        response.setCurrent(1L);
        response.setSize(20L);
        response.setTotal(0L);
        when(externalShortLinkClient.accessRecords(any(ExternalShortLinkAccessRecordRequest.class))).thenReturn(response);

        Optional<PageVO<ShortLinkVisitVO>> page = adapter.fetchAccessRecords(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                1,
                20,
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(page.isPresent());
        assertEquals(0L, page.get().getTotal());
        assertTrue(page.get().getRecords().isEmpty());
    }

    @Test
    void shouldKeepMissingExternalAccessRecordFingerprintsNullable() {
        ExternalShortLinkAccessRecordResponse record = new ExternalShortLinkAccessRecordResponse();
        record.setCreateTime(LocalDateTime.of(2026, 6, 9, 15, 30));
        ExternalShortLinkAccessRecordPageResponse response = new ExternalShortLinkAccessRecordPageResponse();
        response.setCurrent(1L);
        response.setSize(20L);
        response.setTotal(1L);
        response.setRecords(List.of(record));
        when(externalShortLinkClient.accessRecords(any(ExternalShortLinkAccessRecordRequest.class))).thenReturn(response);

        Optional<PageVO<ShortLinkVisitVO>> page = adapter.fetchAccessRecords(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                1,
                20,
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        );

        assertTrue(page.isPresent());
        ShortLinkVisitVO visit = page.get().getRecords().get(0);
        assertEquals("EXTERNAL_SHORT_LINK_VISIT", visit.getEventType());
        assertEquals("external", visit.getStatSource());
        assertNull(visit.getClientIdHash());
        assertNull(visit.getIpHash());
        assertNull(visit.getUserAgentHash());
        assertNull(visit.getReferer());
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
    void shouldFailStrictExternalStatsWhenExternalStatsFails() {
        when(externalShortLinkClient.stats(any(ExternalShortLinkStatsRequest.class)))
                .thenThrow(new BusinessException("external short link stats unavailable"));

        BusinessException error = assertThrows(BusinessException.class, () -> adapter.fetchStatsStrict(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        ));

        assertEquals(502, error.getCode());
        assertEquals("external short link stats unavailable", error.getMessage());
    }

    @Test
    void shouldFailStrictExternalStatsWhenExternalStatsAreDisabled() {
        appProperties.getShortLink().getExternal().setStatsEnabled(false);

        BusinessException error = assertThrows(BusinessException.class, () -> adapter.fetchStatsStrict(
                shortLink("abc123", "http://nurl.ink:8003/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        ));

        assertEquals(502, error.getCode());
        assertTrue(error.getMessage().contains("disabled"));
        verify(externalShortLinkClient, never()).stats(any());
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

    @Test
    void shouldFailStrictExternalStatsWhenDomainDoesNotMatch() {
        BusinessException error = assertThrows(BusinessException.class, () -> adapter.fetchStatsStrict(
                shortLink("abc123", "http://localhost:8080/s/abc123"),
                AdminDateRange.of(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 9))
        ));

        assertEquals(502, error.getCode());
        assertTrue(error.getMessage().contains("domain mismatch"));
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
