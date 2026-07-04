package com.wuxing.persona.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.vo.AdminOverviewVO;
import com.wuxing.persona.vo.NameCountVO;
import com.wuxing.persona.vo.ResultDetailVO;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final Duration SHORT_LINK_TTL = Duration.ofDays(7);
    private static final Duration NULL_SHORT_LINK_TTL = Duration.ofMinutes(5);
    private static final Duration ADMIN_OVERVIEW_TTL = Duration.ofSeconds(45);
    private static final String ADMIN_OVERVIEW_VERSION_KEY = "admin:overview:version";
    private static final String RESULT_PERSONA_RANK_KEY = "admin:rank:persona";
    private static final String RESULT_STAR_OFFICER_RANK_KEY = "admin:rank:star-officer";
    private static final String RESULT_ELEMENT_COMBO_RANK_KEY = "admin:rank:element-combo";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this(redisTemplate, objectMapper, true);
    }

    @Autowired
    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             @Value("${app.cache.redis-enabled:true}") boolean redisEnabled) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
    }

    public ResultDetailVO getResult(String resultId) {
        if (!redisEnabled) {
            return null;
        }
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
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(resultKey(resultId), objectMapper.writeValueAsString(result), RESULT_TTL);
        } catch (Exception ex) {
            log.warn("Write result cache failed, resultId={}", resultId);
        }
    }

    public String getShortLinkResultId(String shortCode) {
        if (!redisEnabled) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(shortLinkKey(shortCode));
        } catch (Exception ex) {
            log.warn("Read short link cache failed, shortCode={}", shortCode);
            return null;
        }
    }

    public void setShortLinkResultId(String shortCode, String resultId) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(shortLinkKey(shortCode), resultId, SHORT_LINK_TTL);
        } catch (Exception ex) {
            log.warn("Write short link cache failed, shortCode={}", shortCode);
        }
    }

    public boolean isNullShortLink(String shortCode) {
        if (!redisEnabled) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(nullShortLinkKey(shortCode)));
        } catch (Exception ex) {
            log.warn("Read null short link cache failed, shortCode={}", shortCode);
            return false;
        }
    }

    public void setNullShortLink(String shortCode) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(nullShortLinkKey(shortCode), "1", NULL_SHORT_LINK_TTL);
        } catch (Exception ex) {
            log.warn("Write null short link cache failed, shortCode={}", shortCode);
        }
    }

    public AdminOverviewVO getAdminOverview(String rangeKey) {
        if (!redisEnabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(adminOverviewKey(rangeKey));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, AdminOverviewVO.class);
        } catch (Exception ex) {
            log.warn("Read admin overview cache failed, rangeKey={}", rangeKey);
            return null;
        }
    }

    public void setAdminOverview(String rangeKey, AdminOverviewVO overview) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(adminOverviewKey(rangeKey), objectMapper.writeValueAsString(overview), ADMIN_OVERVIEW_TTL);
        } catch (Exception ex) {
            log.warn("Write admin overview cache failed, rangeKey={}", rangeKey);
        }
    }

    public void evictAdminOverview() {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().increment(ADMIN_OVERVIEW_VERSION_KEY);
        } catch (Exception ex) {
            log.warn("Evict admin overview cache failed");
        }
    }

    public void incrementResultLeaderboards(String personaLabel, String starOfficerName, String elementCombo) {
        if (!redisEnabled) {
            return;
        }
        try {
            incrementRank(RESULT_PERSONA_RANK_KEY, personaLabel);
            incrementRank(RESULT_STAR_OFFICER_RANK_KEY, starOfficerName);
            incrementRank(RESULT_ELEMENT_COMBO_RANK_KEY, elementCombo);
        } catch (Exception ex) {
            log.warn("Write result leaderboard cache failed, personaLabel={}, starOfficerName={}, elementCombo={}",
                    personaLabel, starOfficerName, elementCombo);
        }
    }

    public List<NameCountVO> topPersonaLeaderboards(int limit) {
        return topRank(RESULT_PERSONA_RANK_KEY, limit);
    }

    public List<NameCountVO> topStarOfficerLeaderboards(int limit) {
        return topRank(RESULT_STAR_OFFICER_RANK_KEY, limit);
    }

    public List<NameCountVO> topElementComboLeaderboards(int limit) {
        return topRank(RESULT_ELEMENT_COMBO_RANK_KEY, limit);
    }

    private void incrementRank(String key, String member) {
        if (member == null || member.isBlank()) {
            return;
        }
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        if (zSetOperations == null) {
            return;
        }
        zSetOperations.incrementScore(key, member.trim(), 1D);
    }

    private List<NameCountVO> topRank(String key, int limit) {
        if (!redisEnabled) {
            return List.of();
        }
        try {
            ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
            if (zSetOperations == null) {
                return List.of();
            }
            Set<ZSetOperations.TypedTuple<String>> rows = zSetOperations.reverseRangeWithScores(key, 0,
                    Math.max(0, limit - 1L));
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            return rows.stream()
                    .filter(row -> row.getValue() != null && row.getScore() != null)
                    .map(row -> new NameCountVO(row.getValue(), Math.round(row.getScore())))
                    .toList();
        } catch (Exception ex) {
            log.warn("Read result leaderboard cache failed, key={}", key);
            return List.of();
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

    private String adminOverviewKey(String rangeKey) {
        return "admin:overview:v" + adminOverviewVersion() + ':' + rangeKey;
    }

    private String adminOverviewVersion() {
        try {
            String version = redisTemplate.opsForValue().get(ADMIN_OVERVIEW_VERSION_KEY);
            return version == null || version.isBlank() ? "0" : version;
        } catch (Exception ex) {
            log.warn("Read admin overview cache version failed");
            return "0";
        }
    }
}
