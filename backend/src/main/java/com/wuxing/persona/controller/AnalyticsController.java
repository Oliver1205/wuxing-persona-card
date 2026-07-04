package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.dto.AnalyticsEventRequest;
import com.wuxing.persona.dto.AnalyticsSessionRequest;
import com.wuxing.persona.service.AnalyticsRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private static final int MAX_TRANSIENT_RETRIES = 3;

    private final AnalyticsRealtimeService analyticsRealtimeService;

    public AnalyticsController(AnalyticsRealtimeService analyticsRealtimeService) {
        this.analyticsRealtimeService = analyticsRealtimeService;
    }

    @PostMapping("/session/start")
    public ApiResponse<Void> startSession(@RequestBody(required = false) AnalyticsSessionRequest request,
                                          @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                          @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                          HttpServletRequest servletRequest) {
        runBestEffort("session_start",
                () -> analyticsRealtimeService.startSession(nonNull(request), clientId, sessionId, servletRequest));
        return ApiResponse.success();
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@RequestBody(required = false) AnalyticsSessionRequest request,
                                       @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                       @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                       HttpServletRequest servletRequest) {
        runBestEffort("heartbeat",
                () -> analyticsRealtimeService.heartbeat(nonNull(request), clientId, sessionId, servletRequest));
        return ApiResponse.success();
    }

    @PostMapping("/event")
    public ApiResponse<Void> event(@RequestBody AnalyticsEventRequest request,
                                   @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                   @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                   HttpServletRequest servletRequest) {
        runBestEffort("event", () -> analyticsRealtimeService.recordEvent(request, clientId, sessionId, servletRequest));
        return ApiResponse.success();
    }

    @PostMapping("/session/end")
    public ApiResponse<Void> endSession(@RequestBody(required = false) AnalyticsSessionRequest request,
                                        @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                        @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                        HttpServletRequest servletRequest) {
        runBestEffort("session_end",
                () -> analyticsRealtimeService.endSession(nonNull(request), clientId, sessionId, servletRequest));
        return ApiResponse.success();
    }

    private void runBestEffort(String operationName, Runnable operation) {
        TransientDataAccessException lastTransientFailure = null;
        for (int attempt = 1; attempt <= MAX_TRANSIENT_RETRIES; attempt++) {
            try {
                operation.run();
                return;
            } catch (TransientDataAccessException ex) {
                lastTransientFailure = ex;
                backoff(attempt);
            }
        }
        log.warn("Skip analytics {} after {} transient database conflicts: {}",
                operationName, MAX_TRANSIENT_RETRIES, lastTransientFailure.getMessage());
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(25L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private AnalyticsSessionRequest nonNull(AnalyticsSessionRequest request) {
        return request == null ? new AnalyticsSessionRequest() : request;
    }
}
