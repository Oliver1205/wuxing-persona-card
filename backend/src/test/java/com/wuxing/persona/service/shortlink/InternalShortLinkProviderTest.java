package com.wuxing.persona.service.shortlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.service.RedisCacheService;
import com.wuxing.persona.service.VisitEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalShortLinkProviderTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private VisitEventService visitEventService;

    @Mock
    private HttpServletRequest request;

    private InternalShortLinkProvider provider;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://example.com/");
        provider = new InternalShortLinkProvider(
                shortLinkMapper,
                redisCacheService,
                visitEventService,
                appProperties
        );
    }

    @Test
    void createForResultShouldReuseExistingShortLink() {
        ShortLinkEntity existing = new ShortLinkEntity();
        existing.setResultId("R1");
        existing.setShortCode("abc123");
        when(shortLinkMapper.selectByResultId("R1")).thenReturn(existing);

        ShortLinkEntity result = provider.createForResult("R1");

        assertSame(existing, result);
        verify(shortLinkMapper, never()).countByShortCode(anyString());
        verify(shortLinkMapper, never()).insert(any(ShortLinkEntity.class));
        verify(redisCacheService, never()).setShortLinkResultId(anyString(), anyString());
    }

    @Test
    void createForResultShouldRetryWhenGeneratedCodeCollides() {
        when(shortLinkMapper.selectByResultId("R2")).thenReturn(null);
        when(shortLinkMapper.countByShortCode(anyString())).thenReturn(1L, 0L);

        ShortLinkEntity result = provider.createForResult("R2");

        ArgumentCaptor<ShortLinkEntity> captor = ArgumentCaptor.forClass(ShortLinkEntity.class);
        verify(shortLinkMapper, times(2)).countByShortCode(anyString());
        verify(shortLinkMapper).insert(captor.capture());
        ShortLinkEntity inserted = captor.getValue();
        assertEquals("R2", inserted.getResultId());
        assertEquals("/result/R2", inserted.getOriginalPath());
        assertEquals("https://example.com/s/" + inserted.getShortCode(), inserted.getShortUrl());
        assertEquals(6, inserted.getShortCode().length());
        assertEquals(inserted.getShortCode(), result.getShortCode());
        verify(redisCacheService).setShortLinkResultId(inserted.getShortCode(), "R2");
    }

    @Test
    void resolveAndRecordShouldReturnNullWhenNullCacheHit() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(true);

        String resultId = provider.resolveAndRecord("abc123", "client-a", request);

        assertNull(resultId);
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(visitEventService, never()).recordAsync(any(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void resolveAndRecordShouldWriteNullCacheWhenDbMisses() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn(null);
        when(shortLinkMapper.selectByShortCode("abc123")).thenReturn(null);

        String resultId = provider.resolveAndRecord("abc123", "client-a", request);

        assertNull(resultId);
        verify(redisCacheService).setNullShortLink("abc123");
        verify(visitEventService, never()).recordAsync(any(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void resolveAndRecordShouldRejectMalformedShortCode() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> provider.resolveAndRecord("bad-code", "client-a", request)
        );

        assertEquals("shortCode must be base62 and length 6 or 7", exception.getMessage());
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(redisCacheService, never()).setNullShortLink(anyString());
    }

    @Test
    void resolveAndRecordShouldUseRedisMappingWithoutRealtimeAggregation() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn("R3");

        String resultId = provider.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R3", resultId);
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(visitEventService).recordAsync(eq(EventType.SHORT_LINK_VISIT),
                eq("/s/abc123"), eq("R3"), eq("abc123"), eq("client-a"), eq(request));
        verify(shortLinkMapper).touchLastVisitAtIfStale(eq("abc123"), any(), any());
        verify(shortLinkMapper, never()).touchLastVisitAt(eq("abc123"), any());
        verify(shortLinkMapper, never()).updateCounters(anyString(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void resolveAndRecordShouldUseConfiguredLastVisitTouchInterval() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://example.com/");
        appProperties.getShortLink().setLastVisitTouchIntervalSeconds(120);
        InternalShortLinkProvider customProvider = new InternalShortLinkProvider(
                shortLinkMapper,
                redisCacheService,
                visitEventService,
                appProperties
        );
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn("R5");

        String resultId = customProvider.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R5", resultId);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> staleBeforeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(shortLinkMapper).touchLastVisitAtIfStale(eq("abc123"),
                nowCaptor.capture(), staleBeforeCaptor.capture());
        long intervalSeconds = Duration.between(staleBeforeCaptor.getValue(), nowCaptor.getValue()).getSeconds();
        assertTrue(intervalSeconds >= 119 && intervalSeconds <= 121);
    }

    @Test
    void resolveAndRecordShouldKeepRedirectWhenLastVisitTouchFails() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn("R4");
        doThrow(new RuntimeException("database busy"))
                .when(shortLinkMapper).touchLastVisitAtIfStale(eq("abc123"), any(), any());

        String resultId = provider.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R4", resultId);
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(visitEventService).recordAsync(eq(EventType.SHORT_LINK_VISIT),
                eq("/s/abc123"), eq("R4"), eq("abc123"), eq("client-a"), eq(request));
    }

    private ShortLinkEntity shortLink(String resultId, String shortCode) {
        ShortLinkEntity entity = new ShortLinkEntity();
        entity.setResultId(resultId);
        entity.setShortCode(shortCode);
        entity.setShortUrl("https://example.com/s/" + shortCode);
        return entity;
    }
}
