package com.wuxing.persona.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.vo.ResultDetailVO;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final Duration SHORT_LINK_TTL = Duration.ofDays(7);
    private static final Duration NULL_SHORT_LINK_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public ResultDetailVO getResult(String resultId) {
        try {
            String json = redisTemplate.opsForValue().get(resultKey(resultId));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, ResultDetailVO.class);
        } catch (Exception ex) {
            log.warn("Read result cache failed, resultId={}", resultId);
            return null;
        }
    }

    public void setResult(String resultId, ResultDetailVO result) {
        try {
            redisTemplate.opsForValue().set(resultKey(resultId), objectMapper.writeValueAsString(result), RESULT_TTL);
        } catch (Exception ex) {
            log.warn("Write result cache failed, resultId={}", resultId);
        }
    }

    public String getShortLinkResultId(String shortCode) {
        try {
            return redisTemplate.opsForValue().get(shortLinkKey(shortCode));
        } catch (Exception ex) {
            log.warn("Read short link cache failed, shortCode={}", shortCode);
            return null;
        }
    }

    public void setShortLinkResultId(String shortCode, String resultId) {
        try {
            redisTemplate.opsForValue().set(shortLinkKey(shortCode), resultId, SHORT_LINK_TTL);
        } catch (Exception ex) {
            log.warn("Write short link cache failed, shortCode={}", shortCode);
        }
    }

    public boolean isNullShortLink(String shortCode) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(nullShortLinkKey(shortCode)));
        } catch (Exception ex) {
            log.warn("Read null short link cache failed, shortCode={}", shortCode);
            return false;
        }
    }

    public void setNullShortLink(String shortCode) {
        try {
            redisTemplate.opsForValue().set(nullShortLinkKey(shortCode), "1", NULL_SHORT_LINK_TTL);
        } catch (Exception ex) {
            log.warn("Write null short link cache failed, shortCode={}", shortCode);
        }
    }

    private String resultKey(String resultId) {
        return "result:" + resultId;
    }

    private String shortLinkKey(String shortCode) {
        return "shortlink:code:" + shortCode;
    }

    private String nullShortLinkKey(String shortCode) {
        return "shortlink:null:" + shortCode;
    }
}
