package com.wuxing.persona.service;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.enums.ShortLinkMode;
import com.wuxing.persona.service.shortlink.ExternalShortLinkProvider;
import com.wuxing.persona.service.shortlink.InternalShortLinkProvider;
import com.wuxing.persona.service.shortlink.ShortLinkCodeUtils;
import com.wuxing.persona.service.shortlink.ShortLinkProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ShortLinkService {

    private final AppProperties appProperties;
    private final InternalShortLinkProvider internalShortLinkProvider;
    private final ExternalShortLinkProvider externalShortLinkProvider;

    public ShortLinkService(AppProperties appProperties,
                            InternalShortLinkProvider internalShortLinkProvider,
                            ExternalShortLinkProvider externalShortLinkProvider) {
        this.appProperties = appProperties;
        this.internalShortLinkProvider = internalShortLinkProvider;
        this.externalShortLinkProvider = externalShortLinkProvider;
    }

    public ShortLinkEntity createForResult(String resultId) {
        return activeProvider().createForResult(resultId);
    }

    public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
        return activeProvider().resolveAndRecord(shortCode, clientId, request);
    }

    public ShortLinkEntity getByResultId(String resultId) {
        return activeProvider().getByResultId(resultId);
    }

    public ShortLinkEntity getByShortCode(String shortCode) {
        ShortLinkCodeUtils.validate(shortCode);
        return activeProvider().getByShortCode(shortCode);
    }

    ShortLinkProvider activeProvider() {
        ShortLinkMode mode = ShortLinkMode.from(appProperties.getShortLink().getMode());
        if (mode == ShortLinkMode.EXTERNAL) {
            return externalShortLinkProvider;
        }
        return internalShortLinkProvider;
    }
}
