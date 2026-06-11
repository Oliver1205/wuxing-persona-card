package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.VisitEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisitEventServiceTest {

    @Mock
    private VisitEventMapper visitEventMapper;

    @Mock
    private HttpServletRequest request;

    private VisitEventService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setHashSalt("test-salt");
        service = new VisitEventService(visitEventMapper, appProperties);
    }

    @Test
    void recordShouldStripQueryAndFragmentFromAbsoluteReferer() {
        stubRequest("https://example.com/result/R1?sc=abc123&token=secret#share");

        service.record(EventType.RESULT_VIEW, "/result/R1", "R1", "abc123", "client-a", request);

        VisitEventEntity entity = capturedEntity();
        assertEquals("https://example.com/result/R1", entity.getReferer());
    }

    @Test
    void recordShouldStripQueryAndFragmentFromRelativeReferer() {
        stubRequest("/result/R2?sc=abc123#card");

        service.record(EventType.RESULT_VIEW, "/result/R2", "R2", "abc123", "client-a", request);

        VisitEventEntity entity = capturedEntity();
        assertEquals("/result/R2", entity.getReferer());
    }

    @Test
    void recordShouldNotBreakMainFlowWhenInsertFails() {
        stubRequest("/s/abc123?channel=share");
        doThrow(new RuntimeException("database busy")).when(visitEventMapper).insert(any());

        assertDoesNotThrow(() ->
                service.record(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request));
    }

    private void stubRequest(String referer) {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case "Referer" -> referer;
            case "User-Agent" -> "JUnit";
            default -> null;
        });
    }

    private VisitEventEntity capturedEntity() {
        ArgumentCaptor<VisitEventEntity> captor = ArgumentCaptor.forClass(VisitEventEntity.class);
        verify(visitEventMapper).insert(captor.capture());
        return captor.getValue();
    }
}
