package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.entity.ShortLinkEntity;
import jakarta.servlet.http.HttpServletRequest;

public interface ShortLinkProvider {

    ShortLinkEntity createForResult(String resultId);

    String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request);

    ShortLinkEntity getByResultId(String resultId);

    ShortLinkEntity getByShortCode(String shortCode);
}
