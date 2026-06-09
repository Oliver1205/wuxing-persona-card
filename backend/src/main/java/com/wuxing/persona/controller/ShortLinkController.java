package com.wuxing.persona.controller;

import com.wuxing.persona.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        redirectTo(response, "/result/" + resultId + "?sc=" + shortCode);
    }

    private void redirectTo(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", location);
    }
}
