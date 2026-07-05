package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.wuxing.persona.mapper.ShortLinkDailyMetricMapper;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.SiteDailyMetricMapper;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsAdapter;
import com.wuxing.persona.vo.NameCountVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminStatServiceTest {

    private final AdminStatService service = new AdminStatService(
            mock(UserResultMapper.class),
            mock(ShortLinkMapper.class),
            mock(VisitEventMapper.class),
            mock(SiteDailyMetricMapper.class),
            mock(ShortLinkDailyMetricMapper.class),
            mock(ExternalShortLinkStatsAdapter.class),
            mock(RedisCacheService.class)
    );

    @Test
    void toNameCountsShouldAcceptProductionExpressionAlias() {
        List<NameCountVO> result = invokeToNameCounts(List.of(Map.of(
                "COALESCE(NULLIF(persona_label, ''), persona_type_id)", "水岸的灯",
                "COUNT", 3L
        )));

        assertEquals("水岸的灯", result.get(0).getName());
        assertEquals(3L, result.get(0).getCount());
    }

    @Test
    void toNameCountsShouldFallbackWhenDisplayNameIsBlank() {
        Map<String, Object> row = new HashMap<>();
        row.put("name", " ");
        row.put("count", 2L);

        List<NameCountVO> result = invokeToNameCounts(List.of(row));

        assertEquals("未标记", result.get(0).getName());
        assertEquals(2L, result.get(0).getCount());
    }

    @SuppressWarnings("unchecked")
    private List<NameCountVO> invokeToNameCounts(List<Map<String, Object>> rows) {
        return (List<NameCountVO>) ReflectionTestUtils.invokeMethod(service, "toNameCounts", rows);
    }
}
