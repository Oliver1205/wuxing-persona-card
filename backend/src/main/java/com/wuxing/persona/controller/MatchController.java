package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.dto.CreateMatchRequest;
import com.wuxing.persona.service.MatchService;
import com.wuxing.persona.vo.MatchCandidateVO;
import com.wuxing.persona.vo.MatchResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/candidates/{shortCode}")
    public ApiResponse<MatchCandidateVO> candidate(@PathVariable String shortCode) {
        return ApiResponse.success(matchService.candidate(shortCode));
    }

    @PostMapping
    public ApiResponse<MatchResultVO> create(@Valid @RequestBody CreateMatchRequest request,
                                             @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(matchService.create(request, clientId, servletRequest));
    }

    @GetMapping("/{partnerShortCode}/{currentShortCode}")
    public ApiResponse<MatchResultVO> get(@PathVariable String partnerShortCode,
                                          @PathVariable String currentShortCode) {
        return ApiResponse.success(matchService.get(partnerShortCode, currentShortCode));
    }
}
