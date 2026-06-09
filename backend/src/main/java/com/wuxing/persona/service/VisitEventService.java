package com.wuxing.persona.service;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.util.HashUtils;
import com.wuxing.persona.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class VisitEventService {

    private final VisitEventMapper visitEventMapper;
    private final AppProperties appProperties;

    public VisitEventService(VisitEventMapper visitEventMapper, AppProperties appProperties) {
        this.visitEventMapper = visitEventMapper;
        this.appProperties = appProperties;
    }

    public void record(EventType eventType,
                       String pagePath,
                       String resultId,
                       String shortCode,
                       String clientId,
                       HttpServletRequest request) {
        VisitEventEntity entity = new VisitEventEntity();
        entity.setEventType(eventType.name());
        entity.setPagePath(pagePath);
        entity.setResultId(resultId);
        entity.setShortCode(shortCode);
        String ip = IpUtils.actualIp(request);
        String userAgent = request.getHeader("User-Agent");
        String clientHashSource = clientId;
        if (clientHashSource == null || clientHashSource.isBlank()) {
            clientHashSource = ip + "|" + userAgent;
        }
        entity.setClientIdHash(HashUtils.sha256(clientHashSource + appProperties.getHashSalt()));
        entity.setIpHash(HashUtils.sha256(ip + appProperties.getHashSalt()));
        entity.setUserAgentHash(HashUtils.sha256((userAgent == null ? "" : userAgent) + appProperties.getHashSalt()));
        entity.setReferer(request.getHeader("Referer"));
        entity.setCreatedAt(LocalDateTime.now());
        visitEventMapper.insert(entity);
    }
}
