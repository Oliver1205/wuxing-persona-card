package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.service.AdminDateRange;
import com.wuxing.persona.service.AdminStatService;
import com.wuxing.persona.service.shortlink.ExternalShortLinkRuntimeService;
import com.wuxing.persona.vo.AdminShortLinkExportVO;
import com.wuxing.persona.vo.AdminOverviewVO;
import com.wuxing.persona.vo.ExternalShortLinkRuntimeVO;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.ShortLinkListItemVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminStatService adminStatService;
    private final AppProperties appProperties;
    private final ExternalShortLinkRuntimeService externalShortLinkRuntimeService;

    public AdminController(AdminStatService adminStatService,
                           AppProperties appProperties,
                           ExternalShortLinkRuntimeService externalShortLinkRuntimeService) {
        this.adminStatService = adminStatService;
        this.appProperties = appProperties;
        this.externalShortLinkRuntimeService = externalShortLinkRuntimeService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewVO> overview(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        checkToken(token);
        return ApiResponse.success(adminStatService.overview(AdminDateRange.of(startDate, endDate)));
    }

    @GetMapping("/short-links")
    public ApiResponse<PageVO<ShortLinkListItemVO>> shortLinks(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                               @RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long pageSize,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String statSource,
                                                               @RequestParam(required = false)
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam(required = false)
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        checkToken(token);
        return ApiResponse.success(adminStatService.listShortLinks(page, pageSize, AdminDateRange.of(startDate, endDate),
                keyword, statSource));
    }

    @GetMapping("/short-links/export")
    public ResponseEntity<String> exportShortLinks(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String statSource,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        checkToken(token);
        AdminShortLinkExportVO export = adminStatService.exportShortLinks(AdminDateRange.of(startDate, endDate),
                keyword, statSource);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.getFilename() + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(export.getContent());
    }

    @GetMapping("/short-links/{shortCode}/visits")
    public ApiResponse<PageVO<ShortLinkVisitVO>> visits(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                        @PathVariable String shortCode,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long pageSize,
                                                        @RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        checkToken(token);
        return ApiResponse.success(adminStatService.listShortLinkVisits(shortCode, page, pageSize,
                AdminDateRange.of(startDate, endDate)));
    }

    @GetMapping("/external-shortlink/status")
    public ApiResponse<ExternalShortLinkRuntimeVO> externalShortLinkStatus(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(defaultValue = "false") boolean probe) {
        checkToken(token);
        return ApiResponse.success(externalShortLinkRuntimeService.status(probe));
    }

    private void checkToken(String token) {
        if (!constantTimeEquals(appProperties.getAdminToken(), token)) {
            throw new BusinessException(401, "admin token is invalid");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
