package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;

    @Mock
    private VisitEventMapper visitEventMapper;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private VisitEventService visitEventService;

    @Mock
    private HttpServletRequest request;

    private ShortLinkService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://example.com/");
        service = new ShortLinkService(shortLinkMapper, visitEventMapper, redisCacheService, visitEventService, appProperties);
    }

    @Test
    void createForResultShouldReuseExistingShortLink() {
        ShortLinkEntity existing = new ShortLinkEntity();
        existing.setResultId("R1");
        existing.setShortCode("abc123");
        when(shortLinkMapper.selectByResultId("R1")).thenReturn(existing);

        ShortLinkEntity result = service.createForResult("R1");

        assertSame(existing, result);
        verify(shortLinkMapper, never()).countByShortCode(anyString());
        verify(shortLinkMapper, never()).insert(any(ShortLinkEntity.class));
        verify(redisCacheService, never()).setShortLinkResultId(anyString(), anyString());
    }

    @Test
    void createForResultShouldRetryWhenGeneratedCodeCollides() {
        when(shortLinkMapper.selectByResultId("R2")).thenReturn(null);
        when(shortLinkMapper.countByShortCode(anyString())).thenReturn(1L, 0L);

        ShortLinkEntity result = service.createForResult("R2");

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

        String resultId = service.resolveAndRecord("abc123", "client-a", request);

        assertNull(resultId);
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(visitEventService, never()).record(any(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void resolveAndRecordShouldWriteNullCacheWhenDbMisses() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn(null);
        when(shortLinkMapper.selectByShortCode("abc123")).thenReturn(null);

        String resultId = service.resolveAndRecord("abc123", "client-a", request);

        assertNull(resultId);
        verify(redisCacheService).setNullShortLink("abc123");
        verify(visitEventService, never()).record(any(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void resolveAndRecordShouldRejectMalformedShortCode() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.resolveAndRecord("bad-code", "client-a", request)
        );

        assertEquals("shortCode must be base62 and length 6 or 7", exception.getMessage());
        verify(shortLinkMapper, never()).selectByShortCode(anyString());
        verify(redisCacheService, never()).setNullShortLink(anyString());
    }

    @Test
    void resolveAndRecordShouldUseRedisMappingAndUpdateCounters() {
        when(redisCacheService.isNullShortLink("abc123")).thenReturn(false);
        when(redisCacheService.getShortLinkResultId("abc123")).thenReturn("R3");
        when(shortLinkMapper.selectByShortCode("abc123")).thenReturn(shortLink("R3", "abc123"));
        when(visitEventMapper.countPvByShortCode("abc123")).thenReturn(3L);
        when(visitEventMapper.countUvByShortCode("abc123")).thenReturn(2L);
        when(visitEventMapper.countUipByShortCode("abc123")).thenReturn(1L);

        String resultId = service.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R3", resultId);
        verify(visitEventService).record(eq(com.wuxing.persona.enums.EventType.SHORT_LINK_VISIT),
                eq("/s/abc123"), eq("R3"), eq("abc123"), eq("client-a"), eq(request));
        verify(shortLinkMapper).updateCounters(eq("abc123"), eq(3L), eq(2L), eq(1L), any());
    }

    private ShortLinkEntity shortLink(String resultId, String shortCode) {
        ShortLinkEntity entity = new ShortLinkEntity();
        entity.setResultId(resultId);
        entity.setShortCode(shortCode);
        entity.setShortUrl("https://example.com/s/" + shortCode);
        return entity;
    }
}
