package com.wuxing.persona.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.dto.AnalyticsSessionRequest;
import com.wuxing.persona.service.AnalyticsRealtimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsRealtimeService analyticsRealtimeService;

    @Test
    void shouldRetryTransientAnalyticsHeartbeatFailures() {
        AnalyticsController controller = new AnalyticsController(analyticsRealtimeService);
        DeadlockLoserDataAccessException deadlock = new DeadlockLoserDataAccessException("deadlock", null);
        doThrow(deadlock)
                .doThrow(deadlock)
                .doNothing()
                .when(analyticsRealtimeService)
                .heartbeat(any(AnalyticsSessionRequest.class), eq("client-a"), eq("session-a"),
                        any(MockHttpServletRequest.class));

        ApiResponse<Void> response = controller.heartbeat(
                new AnalyticsSessionRequest(),
                "client-a",
                "session-a",
                new MockHttpServletRequest()
        );

        assertEquals(0, response.getCode());
        verify(analyticsRealtimeService, times(3))
                .heartbeat(any(AnalyticsSessionRequest.class), eq("client-a"), eq("session-a"),
                        any(MockHttpServletRequest.class));
    }

    @Test
    void shouldKeepAnalyticsHeartbeatBestEffortAfterRepeatedTransientFailures() {
        AnalyticsController controller = new AnalyticsController(analyticsRealtimeService);
        doThrow(new DeadlockLoserDataAccessException("deadlock", null))
                .when(analyticsRealtimeService)
                .heartbeat(any(AnalyticsSessionRequest.class), eq("client-a"), eq("session-a"),
                        any(MockHttpServletRequest.class));

        ApiResponse<Void> response = controller.heartbeat(
                new AnalyticsSessionRequest(),
                "client-a",
                "session-a",
                new MockHttpServletRequest()
        );

        assertEquals(0, response.getCode());
        verify(analyticsRealtimeService, times(3))
                .heartbeat(any(AnalyticsSessionRequest.class), eq("client-a"), eq("session-a"),
                        any(MockHttpServletRequest.class));
    }
}
