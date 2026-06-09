package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.service.ResultService;
import com.wuxing.persona.vo.ResultDetailVO;
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
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping
    public ApiResponse<ResultDetailVO> create(@Valid @RequestBody CreateResultRequest request,
                                              @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                              HttpServletRequest servletRequest) {
        return ApiResponse.success(resultService.create(request, clientId, servletRequest));
    }

    @GetMapping("/{resultId}")
    public ApiResponse<ResultDetailVO> get(@PathVariable String resultId,
                                           @RequestHeader(value = "X-Client-Id", required = false) String clientId,
                                           HttpServletRequest servletRequest) {
        return ApiResponse.success(resultService.getByResultId(resultId, clientId, servletRequest));
    }
}
