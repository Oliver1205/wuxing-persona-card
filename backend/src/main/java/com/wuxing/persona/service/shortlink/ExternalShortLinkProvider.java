package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.service.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExternalShortLinkProvider implements ShortLinkProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalShortLinkProvider.class);

    private final ShortLinkMapper shortLinkMapper;
    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;
    private final ExternalShortLinkClient externalShortLinkClient;
    private final InternalShortLinkProvider internalShortLinkProvider;

    public ExternalShortLinkProvider(ShortLinkMapper shortLinkMapper,
                                     RedisCacheService redisCacheService,
                                     AppProperties appProperties,
                                     ExternalShortLinkClient externalShortLinkClient,
                                     InternalShortLinkProvider internalShortLinkProvider) {
        this.shortLinkMapper = shortLinkMapper;
        this.redisCacheService = redisCacheService;
        this.appProperties = appProperties;
        this.externalShortLinkClient = externalShortLinkClient;
        this.internalShortLinkProvider = internalShortLinkProvider;
    }

    @Override
    public ShortLinkEntity createForResult(String resultId) {
        ShortLinkEntity existing = shortLinkMapper.selectByResultId(resultId);
        if (existing != null) {
            return existing;
        }
        try {
            ExternalShortLinkCreateResponse response = externalShortLinkClient.create(buildCreateRequest(resultId));
            String shortCode = ShortLinkCodeUtils.extractFromFullShortUrl(response.getFullShortUrl());
            if (shortLinkMapper.countByShortCode(shortCode) > 0) {
                throw new BusinessException("external shortCode already exists in local binding");
            }
            ShortLinkEntity entity = buildExternalBinding(resultId, shortCode, response.getFullShortUrl());
            shortLinkMapper.insert(entity);
            redisCacheService.setShortLinkResultId(shortCode, resultId);
            return entity;
        } catch (BusinessException ex) {
            if (!appProperties.getShortLink().getExternal().isFallbackToInternal()) {
                throw ex;
            }
            log.warn("External short link create failed, fallback to internal provider, resultId={}", resultId);
            return internalShortLinkProvider.createForResult(resultId);
        }
    }

    @Override
    public String resolveAndRecord(String shortCode, String clientId, HttpServletRequest request) {
        return internalShortLinkProvider.resolveAndRecord(shortCode, clientId, request);
    }

    @Override
    public ShortLinkEntity getByResultId(String resultId) {
        return internalShortLinkProvider.getByResultId(resultId);
    }

    @Override
    public ShortLinkEntity getByShortCode(String shortCode) {
        return internalShortLinkProvider.getByShortCode(shortCode);
    }

    private ExternalShortLinkCreateRequest buildCreateRequest(String resultId) {
        AppProperties.ExternalShortLinkProperties external = appProperties.getShortLink().getExternal();
        ExternalShortLinkCreateRequest request = new ExternalShortLinkCreateRequest();
        request.setOriginUrl(appProperties.getBaseUrl() + "/result/" + resultId);
        request.setGid(external.getGroupId());
        request.setCreatedType(0);
        request.setValidDateType(0);
        request.setValidDate(null);
        request.setDescribe("五行人格卡结果 " + resultId);
        return request;
    }

    private ShortLinkEntity buildExternalBinding(String resultId, String shortCode, String shortUrl) {
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
}
