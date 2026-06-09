package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ShortLinkService {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_RETRY = 5;
    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[0-9a-zA-Z]{6,7}$");

    private final SecureRandom secureRandom = new SecureRandom();
    private final ShortLinkMapper shortLinkMapper;
    private final VisitEventMapper visitEventMapper;
    private final RedisCacheService redisCacheService;
    private final VisitEventService visitEventService;
    private final AppProperties appProperties;

    public ShortLinkService(ShortLinkMapper shortLinkMapper,
                            VisitEventMapper visitEventMapper,
                            RedisCacheService redisCacheService,
                            VisitEventService visitEventService,
                            AppProperties appProperties) {
        this.shortLinkMapper = shortLinkMapper;
        this.visitEventMapper = visitEventMapper;
        this.redisCacheService = redisCacheService;
        this.visitEventService = visitEventService;
        this.appProperties = appProperties;
    }

    public ShortLinkEntity createForResult(String resultId) {
        ShortLinkEntity existing = shortLinkMapper.selectByResultId(resultId);
        if (existing != null) {
            return existing;
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            String shortCode = randomCode();
            if (shortLinkMapper.countByShortCode(shortCode) > 0) {
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            ShortLinkEntity entity = new ShortLinkEntity();
            entity.setShortCode(shortCode);
            entity.setResultId(resultId);
            entity.setOriginalPath("/result/" + resultId);
            entity.setShortUrl(appProperties.getBaseUrl() + "/s/" + shortCode);
            entity.setPvCount(0L);
            entity.setUvCount(0L);
            entity.setUipCount(0L);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            shortLinkMapper.insert(entity);
            redisCacheService.setShortLinkResultId(shortCode, resultId);
            return entity;
        }
        throw new BusinessException("short code generation failed, please retry");
    }

    public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
        validateShortCode(shortCode);
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
        visitEventService.record(EventType.SHORT_LINK_VISIT, "/s/" + shortCode, resultId, shortCode, clientId, request);
        long pv = visitEventMapper.countPvByShortCode(shortCode);
        long uv = visitEventMapper.countUvByShortCode(shortCode);
        long uip = visitEventMapper.countUipByShortCode(shortCode);
        shortLinkMapper.updateCounters(shortCode, pv, uv, uip, LocalDateTime.now());
        return entity == null ? resultId : entity.getResultId();
    }

    public ShortLinkEntity getByResultId(String resultId) {
        return shortLinkMapper.selectByResultId(resultId);
    }

    public ShortLinkEntity getByShortCode(String shortCode) {
        return shortLinkMapper.selectByShortCode(shortCode);
    }

    private void validateShortCode(String shortCode) {
        if (shortCode == null || !SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
            throw new BusinessException("shortCode must be base62 and length 6 or 7");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(BASE62.charAt(secureRandom.nextInt(BASE62.length())));
        }
        return builder.toString();
    }
}
