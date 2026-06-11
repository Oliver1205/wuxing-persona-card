package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.EventRequest;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.service.VisitEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final VisitEventService visitEventService;

    public EventController(VisitEventService visitEventService) {
        this.visitEventService = visitEventService;
    }

    @PostMapping
    public ApiResponse<Void> record(@Valid @RequestBody EventRequest request,
                                    @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                    HttpServletRequest servletRequest) {
        EventType eventType;
        try {
            eventType = EventType.fromCode(request.getEventType());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("eventType is invalid");
        }
        visitEventService.record(eventType, request.getPagePath(), request.getResultId(), request.getShortCode(),
                clientId, servletRequest, request.getSessionId(), request.getChannel(), request.getCampaign());
        return ApiResponse.success();
    }
}
