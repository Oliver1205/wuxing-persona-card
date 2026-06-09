package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.service.AdminStatService;
import com.wuxing.persona.vo.AdminOverviewVO;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.ShortLinkListItemVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.util.Objects;
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

    public AdminController(AdminStatService adminStatService, AppProperties appProperties) {
        this.adminStatService = adminStatService;
        this.appProperties = appProperties;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewVO> overview(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        checkToken(token);
        return ApiResponse.success(adminStatService.overview());
    }

    @GetMapping("/short-links")
    public ApiResponse<PageVO<ShortLinkListItemVO>> shortLinks(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                               @RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long pageSize) {
        checkToken(token);
        return ApiResponse.success(adminStatService.listShortLinks(page, pageSize));
    }

    @GetMapping("/short-links/{shortCode}/visits")
    public ApiResponse<PageVO<ShortLinkVisitVO>> visits(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                        @PathVariable String shortCode,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long pageSize) {
        checkToken(token);
        return ApiResponse.success(adminStatService.listShortLinkVisits(shortCode, page, pageSize));
    }

    private void checkToken(String token) {
        if (!Objects.equals(appProperties.getAdminToken(), token)) {
            throw new BusinessException(401, "admin token is invalid");
        }
    }
}
