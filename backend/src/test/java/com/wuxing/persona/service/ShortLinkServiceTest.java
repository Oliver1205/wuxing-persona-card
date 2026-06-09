package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.shortlink.ExternalShortLinkProvider;
import com.wuxing.persona.service.shortlink.InternalShortLinkProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private InternalShortLinkProvider internalShortLinkProvider;

    @Mock
    private ExternalShortLinkProvider externalShortLinkProvider;

    @Mock
    private HttpServletRequest request;

    private AppProperties appProperties;
    private ShortLinkService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new ShortLinkService(appProperties, internalShortLinkProvider, externalShortLinkProvider);
    }

    @Test
    void shouldUseInternalProviderByDefault() {
        ShortLinkEntity expected = new ShortLinkEntity();
        expected.setShortCode("abc123");
        when(internalShortLinkProvider.createForResult("R1")).thenReturn(expected);

        ShortLinkEntity result = service.createForResult("R1");

        assertSame(expected, result);
        verify(externalShortLinkProvider, never()).createForResult("R1");
    }

    @Test
    void shouldUseExternalProviderWhenModeIsExternal() {
        appProperties.getShortLink().setMode("external");
        when(externalShortLinkProvider.resolveAndRecord("abc123", "client-a", request)).thenReturn("R2");

        String resultId = service.resolveAndRecord("abc123", "client-a", request);

        assertEquals("R2", resultId);
        verify(internalShortLinkProvider, never()).resolveAndRecord("abc123", "client-a", request);
    }

    @Test
    void shouldRejectUnknownMode() {
        appProperties.getShortLink().setMode("remote-only");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createForResult("R3"));

        assertEquals("short link mode must be internal or external", exception.getMessage());
    }
}
