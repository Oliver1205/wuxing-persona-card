package com.wuxing.persona.controller;

import com.wuxing.persona.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/s/{shortCode}")
    public void redirect(@PathVariable String shortCode,
                         @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String resultId = shortLinkService.resolveAndRecord(shortCode, clientId, request);
        if (resultId == null) {
            redirectTo(response, "/not-found");
            return;
        }
        redirectTo(response, resultLocation(resultId, shortCode, request));
    }

    private String resultLocation(String resultId, String shortCode, HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/result/" + resultId)
                .queryParam("sc", shortCode);
        appendQueryParam(builder, "channel", firstPresent(request.getParameter("channel"),
                request.getParameter("utm_source"), request.getParameter("source")));
        appendQueryParam(builder, "campaign", firstPresent(request.getParameter("campaign"),
                request.getParameter("utm_campaign")));
        return builder.build().toUriString();
    }

    private void appendQueryParam(UriComponentsBuilder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(name, value.trim());
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void redirectTo(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", location);
    }
}
