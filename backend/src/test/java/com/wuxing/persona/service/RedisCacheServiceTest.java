package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.vo.ResultDetailVO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCacheService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RedisCacheService(redisTemplate, new ObjectMapper());
    }

    @Test
    void resultCacheShouldSerializeAndDeserializeResultDetail() {
        ResultDetailVO result = new ResultDetailVO();
        result.setResultId("R1");
        result.setPrimaryElement("METAL");
        result.setPrimaryElementName("金");
        result.setPrimaryPercent(70);
        result.setSecondaryElement("WATER");
        result.setSecondaryElementName("水");
        result.setSecondaryPercent(30);
        result.setAllElementScores(Map.of("METAL", 90, "WATER", 40));
        result.setKeywords(List.of("清醒", "判断"));

        service.setResult("R1", result);

        verify(valueOperations).set(eq("result:R1"), any(String.class), eq(Duration.ofHours(24)));
    }

    @Test
    void getResultShouldReturnNullWhenCacheMisses() {
        when(valueOperations.get("result:R1")).thenReturn(null);

        assertNull(service.getResult("R1"));
    }

    @Test
    void getResultShouldDeserializeCachedJson() {
        when(valueOperations.get("result:R1")).thenReturn("""
                {
                  "resultId": "R1",
                  "primaryElement": "METAL",
                  "primaryElementName": "金",
                  "primaryPercent": 70,
                  "secondaryElement": "WATER",
                  "secondaryElementName": "水",
                  "secondaryPercent": 30,
                  "keywords": ["清醒", "判断"]
                }
                """);

        ResultDetailVO result = service.getResult("R1");

        assertEquals("R1", result.getResultId());
        assertEquals("METAL", result.getPrimaryElement());
        assertEquals(List.of("清醒", "判断"), result.getKeywords());
    }

    @Test
    void shortLinkCacheShouldUseExpectedKeys() {
        service.setShortLinkResultId("abc123", "R1");
        when(valueOperations.get("shortlink:code:abc123")).thenReturn("R1");

        assertEquals("R1", service.getShortLinkResultId("abc123"));
        verify(valueOperations).set("shortlink:code:abc123", "R1", Duration.ofDays(7));
    }

    @Test
    void nullShortLinkCacheShouldUseExpectedKeys() {
        service.setNullShortLink("abc123");
        when(redisTemplate.hasKey("shortlink:null:abc123")).thenReturn(true);

        assertTrue(service.isNullShortLink("abc123"));
        verify(valueOperations).set("shortlink:null:abc123", "1", Duration.ofMinutes(5));
    }

    @Test
    void cacheFailuresShouldDegradeToMisses() {
        doThrow(new RuntimeException("redis unavailable")).when(valueOperations).get("result:R1");
        doThrow(new RuntimeException("redis unavailable")).when(redisTemplate).hasKey("shortlink:null:abc123");

        assertNull(service.getResult("R1"));
        assertFalse(service.isNullShortLink("abc123"));
    }
}
