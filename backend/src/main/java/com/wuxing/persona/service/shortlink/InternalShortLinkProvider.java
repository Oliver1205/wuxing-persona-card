package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.service.RedisCacheService;
import com.wuxing.persona.service.VisitEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class InternalShortLinkProvider implements ShortLinkProvider {

    private static final long LAST_VISIT_TOUCH_INTERVAL_SECONDS = 30;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ShortLinkMapper shortLinkMapper;
    private final RedisCacheService redisCacheService;
    private final VisitEventService visitEventService;
    private final AppProperties appProperties;

    public InternalShortLinkProvider(ShortLinkMapper shortLinkMapper,
                                     RedisCacheService redisCacheService,
                                     VisitEventService visitEventService,
                                     AppProperties appProperties) {
        this.shortLinkMapper = shortLinkMapper;
        this.redisCacheService = redisCacheService;
        this.visitEventService = visitEventService;
        this.appProperties = appProperties;
    }

    @Override
    public ShortLinkEntity createForResult(String resultId) {
        ShortLinkEntity existing = shortLinkMapper.selectByResultId(resultId);
        if (existing != null) {
            return existing;
        }
        for (int i = 0; i < ShortLinkCodeUtils.MAX_RETRY; i++) {
            String shortCode = randomCode();
            if (shortLinkMapper.countByShortCode(shortCode) > 0) {
                continue;
            }
            ShortLinkEntity entity = buildEntity(resultId, shortCode, appProperties.getBaseUrl() + "/s/" + shortCode);
            shortLinkMapper.insert(entity);
            redisCacheService.setShortLinkResultId(shortCode, resultId);
            return entity;
        }
        throw new BusinessException("short code generation failed, please retry");
    }

    @Override
    public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
        ShortLinkCodeUtils.validate(shortCode);
        if (redisCacheService.isNullShortLink(shortCode)) {
            return null;
        }
        String resultId = redisCacheService.getShortLinkResultId(shortCode);
        ShortLinkEntity entity = null;
        if (resultId == null || resultId.isBlank()) {
            entity = shortLinkMapper.selectByShortCode(shortCode);
            if (entity == null) {
                redisCacheService.setNullShortLink(shortCode);
                return null;
            }
            resultId = entity.getResultId();
            redisCacheService.setShortLinkResultId(shortCode, resultId);
        }
        if (entity == null) {
            entity = shortLinkMapper.selectByShortCode(shortCode);
        }
        visitEventService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/" + shortCode, resultId, shortCode,
                clientId, request);
        touchLastVisitAtIfStale(shortCode);
        return entity == null ? resultId : entity.getResultId();
    }

    @Override
    public ShortLinkEntity getByResultId(String resultId) {
        return shortLinkMapper.selectByResultId(resultId);
    }

    @Override
    public ShortLinkEntity getByShortCode(String shortCode) {
        return shortLinkMapper.selectByShortCode(shortCode);
    }

    private ShortLinkEntity buildEntity(String resultId, String shortCode, String shortUrl) {
        LocalDateTime now = LocalDateTime.now();
        ShortLinkEntity entity = new ShortLinkEntity();
        entity.setShortCode(shortCode);
        entity.setResultId(resultId);
        entity.setOriginalPath("/result/" + resultId);
        entity.setShortUrl(shortUrl);
        entity.setPvCount(0L);
        entity.setUvCount(0L);
        entity.setUipCount(0L);
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ShortLinkCodeUtils.CODE_LENGTH; i++) {
            builder.append(ShortLinkCodeUtils.BASE62.charAt(secureRandom.nextInt(ShortLinkCodeUtils.BASE62.length())));
        }
        return builder.toString();
    }

    private void touchLastVisitAtIfStale(String shortCode) {
        LocalDateTime now = LocalDateTime.now();
        shortLinkMapper.touchLastVisitAtIfStale(shortCode, now, now.minusSeconds(LAST_VISIT_TOUCH_INTERVAL_SECONDS));
    }
}
