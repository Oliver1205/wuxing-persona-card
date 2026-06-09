package com.wuxing.persona.service.shortlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.service.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalShortLinkProviderTest {

    @Mock
    private ShortLinkMapper shortLinkMapper;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private ExternalShortLinkClient externalShortLinkClient;

    @Mock
    private InternalShortLinkProvider internalShortLinkProvider;

    @Mock
    private HttpServletRequest request;

    private AppProperties appProperties;
    private ExternalShortLinkProvider provider;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setBaseUrl("https://wuxing.example.com/");
        appProperties.getShortLink().getExternal().setGroupId("wuxing_persona");
        provider = new ExternalShortLinkProvider(
                shortLinkMapper,
                redisCacheService,
                appProperties,
                externalShortLinkClient,
                internalShortLinkProvider
        );
    }

    @Test
    void createForResultShouldSaveExternalBindingWhenRemoteSucceeds() {
        when(shortLinkMapper.selectByResultId("R1")).thenReturn(null);
        when(shortLinkMapper.countByShortCode("Abc123")).thenReturn(0L);
        ExternalShortLinkCreateResponse response = new ExternalShortLinkCreateResponse();
        response.setGid("wuxing_persona");
        response.setOriginUrl("https://wuxing.example.com/result/R1");
        response.setFullShortUrl("https://s.example.com/Abc123");
        when(externalShortLinkClient.create(any(ExternalShortLinkCreateRequest.class))).thenReturn(response);

        ShortLinkEntity result = provider.createForResult("R1");

        ArgumentCaptor<ExternalShortLinkCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(ExternalShortLinkCreateRequest.class);
        verify(externalShortLinkClient).create(requestCaptor.capture());
        ExternalShortLinkCreateRequest externalRequest = requestCaptor.getValue();
        assertEquals("https://wuxing.example.com/result/R1", externalRequest.getOriginUrl());
        assertEquals("wuxing_persona", externalRequest.getGid());
        assertEquals(0, externalRequest.getCreatedType());
        assertEquals(0, externalRequest.getValidDateType());

        ArgumentCaptor<ShortLinkEntity> entityCaptor = ArgumentCaptor.forClass(ShortLinkEntity.class);
        verify(shortLinkMapper).insert(entityCaptor.capture());
        ShortLinkEntity inserted = entityCaptor.getValue();
        assertEquals("R1", inserted.getResultId());
        assertEquals("Abc123", inserted.getShortCode());
        assertEquals("/result/R1", inserted.getOriginalPath());
        assertEquals("https://s.example.com/Abc123", inserted.getShortUrl());
        assertEquals("Abc123", result.getShortCode());
        verify(redisCacheService).setShortLinkResultId("Abc123", "R1");
        verify(internalShortLinkProvider, never()).createForResult("R1");
    }

    @Test
    void createForResultShouldFallbackToInternalWhenRemoteFails() {
        when(shortLinkMapper.selectByResultId("R2")).thenReturn(null);
        when(externalShortLinkClient.create(any(ExternalShortLinkCreateRequest.class)))
                .thenThrow(new BusinessException("external short link service unavailable"));
        ShortLinkEntity fallback = new ShortLinkEntity();
        fallback.setResultId("R2");
        fallback.setShortCode("abc123");
        when(internalShortLinkProvider.createForResult("R2")).thenReturn(fallback);

        ShortLinkEntity result = provider.createForResult("R2");

        assertSame(fallback, result);
        verify(internalShortLinkProvider).createForResult("R2");
    }

    @Test
    void createForResultShouldFailWhenRemoteFailsAndFallbackDisabled() {
        appProperties.getShortLink().getExternal().setFallbackToInternal(false);
        when(shortLinkMapper.selectByResultId("R3")).thenReturn(null);
        when(externalShortLinkClient.create(any(ExternalShortLinkCreateRequest.class)))
                .thenThrow(new BusinessException("external short link service unavailable"));

        BusinessException exception = assertThrows(BusinessException.class, () -> provider.createForResult("R3"));

        assertEquals("external short link service unavailable", exception.getMessage());
        verify(internalShortLinkProvider, never()).createForResult("R3");
    }

    @Test
    void resolveAndRecordShouldDelegateToInternalProvider() {
        when(internalShortLinkProvider.resolveAndRecord("abc123", "client-a", request)).thenReturn("R4");

        String resultId = provider.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R4", resultId);
    }
}
