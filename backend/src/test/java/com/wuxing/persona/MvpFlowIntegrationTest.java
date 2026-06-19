package com.wuxing.persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.service.AdminDateRange;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsAdapter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:wuxing;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema-local.sql",
        "app.base-url=http://localhost:8080",
        "app.admin-token=test-token",
        "app.hash-salt=test-salt",
        "app.cors.allowed-origins=https://frontend.example.com"
})
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM short_link_daily_metric",
        "DELETE FROM site_daily_metric",
        "DELETE FROM visit_event",
        "DELETE FROM short_link",
        "DELETE FROM user_result"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MvpFlowIntegrationTest {

    private static final long ASYNC_EVENT_TIMEOUT_MS = 10_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter;

    @BeforeEach
    void setUpRedisMock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void shouldExposeReadinessHttpContract() throws Exception {
        mockMvc.perform(get("/api/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.scope").value("core_schema"))
                .andExpect(jsonPath("$.data.coreTables.user_result").value("ok"))
                .andExpect(jsonPath("$.data.coreTables.short_link").value("ok"))
                .andExpect(jsonPath("$.data.coreTables.visit_event").value("ok"))
                .andExpect(jsonPath("$.data.coreTables.site_daily_metric").value("ok"))
                .andExpect(jsonPath("$.data.coreTables.short_link_daily_metric").value("ok"));
    }

    @Test
    void shouldAllowConfiguredCorsPreflightForIndependentFrontendDomain() throws Exception {
        mockMvc.perform(options("/api/questions")
                        .header("Origin", "https://frontend.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type,X-Client-Id,X-Session-Id,X-Channel,X-Campaign,X-Admin-Token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example.com"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Client-Id")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Admin-Token")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void shouldRejectCorsPreflightFromUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/questions")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type,X-Client-Id"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCompleteResultShortLinkAndAdminFlow() throws Exception {
        recordEventApiSmoke();
        seedGrowthEvent("PAGE_VIEW_HOME", "/");
        seedGrowthEvent("START_TEST_CLICK", "/");
        seedGrowthEvent("TEST_FORM_START", "/test");
        seedGrowthEvent("TEST_SUBMIT_ATTEMPT", "/test");
        JsonNode data = createValidResult("client-a");
        String resultId = data.get("resultId").asText();
        String shortCode = data.get("shortCode").asText();
        seedGrowthEvent("SHARE_PANEL_VIEW", "/result/" + resultId);
        seedGrowthEvent("SHORT_LINK_COPY", "/result/" + resultId);
        seedGrowthEvent("SAVE_SHARE_IMAGE_SUCCESS", "/result/" + resultId);
        seedGrowthEvent("NATIVE_SHARE_SUCCESS", "/result/" + resultId);

        mockMvc.perform(get("/api/results/" + resultId)
                        .header("X-Client-Id", "client-a")
                        .header("X-Session-Id", "session-a")
                        .header("X-Channel", "organic")
                        .header("X-Campaign", "spring-launch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultId").value(resultId));

        mockMvc.perform(get("/s/" + shortCode)
                        .param("channel", "share")
                        .param("campaign", "result-card")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "/result/" + resultId + "?sc=" + shortCode + "&channel=share&campaign=result-card"));

        mockMvc.perform(get("/api/admin/overview").header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(1))
                .andExpect(jsonPath("$.data.dailyTrends").isArray())
                .andExpect(jsonPath("$.data.dailyTrends[0].date").exists())
                .andExpect(jsonPath("$.data.funnelSteps[0].eventType").value("PAGE_VIEW_HOME"))
                .andExpect(jsonPath("$.data.funnelSteps[0].count").value(1))
                .andExpect(jsonPath("$.data.funnelSteps[4].eventType").value("TEST_SUBMIT"))
                .andExpect(jsonPath("$.data.funnelSteps[4].count").value(1))
                .andExpect(jsonPath("$.data.funnelSteps[6].eventType").value("SHARE_PANEL_VIEW"))
                .andExpect(jsonPath("$.data.funnelSteps[6].label").value("分享区曝光"))
                .andExpect(jsonPath("$.data.funnelSteps[6].count").value(1))
                .andExpect(jsonPath("$.data.funnelSteps[7].eventType").value("SHORT_LINK_COPY"))
                .andExpect(jsonPath("$.data.funnelSteps[7].count").value(1))
                .andExpect(jsonPath("$.data.funnelSteps[8].eventType").value("SAVE_SHARE_IMAGE_SUCCESS"))
                .andExpect(jsonPath("$.data.funnelSteps[8].count").value(1))
                .andExpect(jsonPath("$.data.funnelSteps[9].eventType").value("NATIVE_SHARE_SUCCESS"))
                .andExpect(jsonPath("$.data.funnelSteps[9].count").value(1))
                .andExpect(jsonPath("$.data.topChannels[0].name").value("organic"))
                .andExpect(jsonPath("$.data.topCampaigns[0].name").value("spring-launch"));

        mockMvc.perform(get("/api/admin/visit-events/runtime").header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queueCapacity").value(2048))
                .andExpect(jsonPath("$.data.drainLimit").value(64))
                .andExpect(jsonPath("$.data.droppedAsyncEvents").value(0))
                .andExpect(jsonPath("$.data.workerAlive").value(true));
    }

    @Test
    void shouldReturnShortLinkListAndVisitDetailStats() throws Exception {
        JsonNode data = createValidResult("client-a");
        String shortCode = data.get("shortCode").asText();
        String resultId = data.get("resultId").asText();

        mockMvc.perform(get("/s/" + shortCode)
                        .param("channel", "share")
                        .param("campaign", "result-card")
                        .header("X-Client-Id", "same-client")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/s/" + shortCode)
                        .param("channel", "share")
                        .param("campaign", "result-card")
                        .header("X-Client-Id", "same-client")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$.data.records[0].pv").value(2))
                .andExpect(jsonPath("$.data.records[0].uv").value(1))
                .andExpect(jsonPath("$.data.records[0].uip").value(1))
                .andExpect(jsonPath("$.data.records[0].metricSource").value("live_event"));

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("keyword", resultId)
                        .param("statSource", "local")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].resultId").value(resultId))
                .andExpect(jsonPath("$.data.records[0].statSource").value("local"))
                .andExpect(jsonPath("$.data.records[0].metricSource").value("live_event"));

        mockMvc.perform(get("/api/admin/short-links/export")
                        .header("X-Admin-Token", "test-token")
                        .param("keyword", shortCode)
                        .param("statSource", "local"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("wuxing-short-links")))
                .andExpect(content().string(containsString(shortCode)))
                .andExpect(content().string(containsString(resultId)))
                .andExpect(content().string(containsString("metricSource")));

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].eventType").value("SHORT_LINK_VISIT"))
                .andExpect(jsonPath("$.data.records[0].clientIdHash").exists())
                .andExpect(jsonPath("$.data.records[0].ipHash").exists())
                .andExpect(jsonPath("$.data.records[0].channel").value("share"))
                .andExpect(jsonPath("$.data.records[0].campaign").value("result-card"))
                .andExpect(jsonPath("$.data.records[0].deviceType").value("mobile"));
    }

    @Test
    void shouldRejectUnboundedComputedSourceFiltering() throws Exception {
        seedLocalShortLinks(501);

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("statSource", "local")
                        .param("page", "26")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("statSource filter scans at most 500 short links")));
    }

    @Test
    void shouldReportExternalStatSourceFailureInsteadOfPretendingLocal() throws Exception {
        createValidResult("client-external-stats-failure");
        when(externalShortLinkStatsAdapter.fetchStatsStrict(
                org.mockito.ArgumentMatchers.any(ShortLinkEntity.class),
                org.mockito.ArgumentMatchers.any(AdminDateRange.class)))
                .thenThrow(new BusinessException(502, "external short link stats unavailable"));

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("statSource", "external")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("external short link stats unavailable"));
    }

    @Test
    void shouldMapBatchedShortLinkStatsToEachListItem() throws Exception {
        JsonNode first = createValidResult("client-a");
        JsonNode second = createValidResult("client-b");
        String firstShortCode = first.get("shortCode").asText();
        String secondShortCode = second.get("shortCode").asText();

        mockMvc.perform(get("/s/" + firstShortCode)
                        .header("X-Client-Id", "same-client"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/s/" + firstShortCode)
                        .header("X-Client-Id", "same-client"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/s/" + secondShortCode)
                        .header("X-Client-Id", "another-client"))
                .andExpect(status().is3xxRedirection());

        MvcResult response = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        JsonNode records = objectMapper.readTree(response.getResponse().getContentAsString()).get("data").get("records");

        JsonNode firstItem = findShortLinkItem(records, firstShortCode);
        JsonNode secondItem = findShortLinkItem(records, secondShortCode);
        assertNotNull(firstItem);
        assertNotNull(secondItem);
        assertEquals(2, firstItem.get("pv").asInt());
        assertEquals(1, firstItem.get("uv").asInt());
        assertEquals(1, firstItem.get("uip").asInt());
        assertEquals("live_event", firstItem.get("metricSource").asText());
        assertEquals(1, secondItem.get("pv").asInt());
        assertEquals(1, secondItem.get("uv").asInt());
        assertEquals(1, secondItem.get("uip").asInt());
        assertEquals("live_event", secondItem.get("metricSource").asText());
    }

    @Test
    void shouldFilterAdminStatsByDateRange() throws Exception {
        JsonNode data = createValidResult("client-a");
        String shortCode = data.get("shortCode").asText();
        mockMvc.perform(get("/s/" + shortCode).header("X-Client-Id", "same-client"))
                .andExpect(status().is3xxRedirection());
        String today = LocalDate.now().toString();
        String future = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(1))
                .andExpect(jsonPath("$.data.dailyTrends[0].date").value(today))
                .andExpect(jsonPath("$.data.dailyTrends[0].resultCreated").value(1))
                .andExpect(jsonPath("$.data.dailyTrends[0].shortLinkCreated").value(1))
                .andExpect(jsonPath("$.data.dailyTrends[0].shortLinkVisits").value(1));

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", future)
                        .param("endDate", future))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(0))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(0))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(0))
                .andExpect(jsonPath("$.data.dailyTrends[0].date").value(future))
                .andExpect(jsonPath("$.data.dailyTrends[0].resultCreated").value(0))
                .andExpect(jsonPath("$.data.dailyTrends[0].shortLinkCreated").value(0))
                .andExpect(jsonPath("$.data.dailyTrends[0].shortLinkVisits").value(0));

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", future)
                        .param("endDate", future))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", future)
                        .param("endDate", future))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldAggregateAnalyticsMetricsForAdmin() throws Exception {
        JsonNode data = createValidResult("client-a");
        String shortCode = data.get("shortCode").asText();
        mockMvc.perform(get("/s/" + shortCode)
                        .header("X-Client-Id", "client-a")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());
        awaitShortLinkVisitCount(shortCode, 1);

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        moveAllRecordsToDate(yesterdayDate);
        String yesterday = yesterdayDate.toString();
        mockMvc.perform(post("/api/admin/analytics/aggregate")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value(yesterday))
                .andExpect(jsonPath("$.data.endDate").value(yesterday))
                .andExpect(jsonPath("$.data.daysAggregated").value(1))
                .andExpect(jsonPath("$.data.shortLinkRowsAggregated").value(1))
                .andExpect(jsonPath("$.data.aggregatedAt").exists());

        mockMvc.perform(post("/api/admin/analytics/aggregate")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortLinkRowsAggregated").value(1));
        verify(valueOperations, atLeast(2)).increment("admin:overview:version");

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday)
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metricSource").value("daily_metric"))
                .andExpect(jsonPath("$.data.aggregatedThroughDate").value(yesterday))
                .andExpect(jsonPath("$.data.dailyTrends[0].date").value(yesterday))
                .andExpect(jsonPath("$.data.dailyTrends[0].shortLinkVisits").value(1))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(1));

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday)
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$.data.records[0].pv").value(1))
                .andExpect(jsonPath("$.data.records[0].uv").value(1))
                .andExpect(jsonPath("$.data.records[0].uip").value(1))
                .andExpect(jsonPath("$.data.records[0].statSource").value("local"))
                .andExpect(jsonPath("$.data.records[0].metricSource").value("daily_metric"));

        String today = LocalDate.now().toString();
        mockMvc.perform(post("/api/admin/analytics/aggregate")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", today)
                        .param("endDate", today))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("aggregation can only include closed dates before today"));
    }

    @Test
    void shouldFallbackToLiveShortLinkStatsWhenClosedDateIsNotAggregated() throws Exception {
        JsonNode data = createValidResult("client-a");
        String shortCode = data.get("shortCode").asText();
        mockMvc.perform(get("/s/" + shortCode)
                        .header("X-Client-Id", "client-a")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());
        awaitShortLinkVisitCount(shortCode, 1);

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        moveAllRecordsToDate(yesterdayDate);
        String yesterday = yesterdayDate.toString();
        Long livePv = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visit_event WHERE event_type = 'SHORT_LINK_VISIT' AND short_code = ? AND created_at >= ? AND created_at < ?",
                Long.class,
                shortCode,
                yesterdayDate.atStartOfDay(),
                yesterdayDate.plusDays(1).atStartOfDay());
        assertEquals(1, livePv == null ? 0 : livePv);

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$.data.records[0].pv").value(1))
                .andExpect(jsonPath("$.data.records[0].uv").value(1))
                .andExpect(jsonPath("$.data.records[0].uip").value(1))
                .andExpect(jsonPath("$.data.records[0].metricSource").value("live_event"));
    }

    @Test
    void shouldKeepDailyMetricSourceForAggregatedZeroVisitShortLinks() throws Exception {
        JsonNode visited = createValidResult("client-a");
        String visitedShortCode = visited.get("shortCode").asText();
        mockMvc.perform(get("/s/" + visitedShortCode)
                        .header("X-Client-Id", "client-a")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());
        awaitShortLinkVisitCount(visitedShortCode, 1);
        JsonNode unvisited = createValidResult("client-b");
        String unvisitedShortCode = unvisited.get("shortCode").asText();

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        moveAllRecordsToDate(yesterdayDate);
        String yesterday = yesterdayDate.toString();
        mockMvc.perform(post("/api/admin/analytics/aggregate")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday)
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("data")
                .get("records");
        JsonNode unvisitedItem = findShortLinkItem(records, unvisitedShortCode);
        assertNotNull(unvisitedItem);
        assertEquals(0, unvisitedItem.get("pv").asLong());
        assertEquals(0, unvisitedItem.get("uv").asLong());
        assertEquals(0, unvisitedItem.get("uip").asLong());
        assertEquals("daily_metric", unvisitedItem.get("metricSource").asText());
    }

    @Test
    void shouldExcludeSyntheticTrafficFromDefaultAdminViews() throws Exception {
        JsonNode organic = createValidResult("client-organic", "organic", "spring-launch");
        JsonNode synthetic = createValidResult("client-perf", "perf-test", "performance-limit-test");
        String organicShortCode = organic.get("shortCode").asText();
        String syntheticShortCode = synthetic.get("shortCode").asText();

        MvcResult defaultOverviewResult = mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syntheticTrafficExcluded").value(true))
                .andExpect(jsonPath("$.data.syntheticIsolationLevel").value("event_channel"))
                .andExpect(jsonPath("$.data.syntheticIsolationNote").value(containsString("不是 user_result/short_link 实体层强隔离")))
                .andExpect(jsonPath("$.data.resultCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(1))
                .andExpect(jsonPath("$.data.topChannels[0].name").value("organic"))
                .andReturn();
        JsonNode defaultOverview = objectMapper.readTree(defaultOverviewResult.getResponse().getContentAsString())
                .get("data");
        assertEquals(false, containsName(defaultOverview.get("topChannels"), "perf-test"));

        MvcResult includeSyntheticOverviewResult = mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syntheticTrafficExcluded").value(false))
                .andExpect(jsonPath("$.data.syntheticIsolationLevel").value("all_traffic"))
                .andExpect(jsonPath("$.data.resultCreated").value(2))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(2))
                .andReturn();
        JsonNode includeSyntheticOverview = objectMapper.readTree(
                        includeSyntheticOverviewResult.getResponse().getContentAsString())
                .get("data");
        assertTrue(containsName(includeSyntheticOverview.get("topChannels"), "perf-test"));

        MvcResult defaultListResult = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();
        JsonNode defaultRecords = objectMapper.readTree(defaultListResult.getResponse().getContentAsString())
                .get("data")
                .get("records");
        assertTrue(containsShortCode(defaultRecords, organicShortCode));
        assertEquals(false, containsShortCode(defaultRecords, syntheticShortCode));

        MvcResult includeSyntheticListResult = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        JsonNode includeSyntheticRecords = objectMapper.readTree(
                        includeSyntheticListResult.getResponse().getContentAsString())
                .get("data")
                .get("records");
        assertTrue(containsShortCode(includeSyntheticRecords, organicShortCode));
        assertTrue(containsShortCode(includeSyntheticRecords, syntheticShortCode));

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        moveAllRecordsToDate(yesterdayDate);
        String yesterday = yesterdayDate.toString();
        mockMvc.perform(post("/api/admin/analytics/aggregate")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syntheticTrafficExcluded").value(true))
                .andExpect(jsonPath("$.data.syntheticIsolationLevel").value("event_channel"))
                .andExpect(jsonPath("$.data.metricSource").value("live_event"))
                .andExpect(jsonPath("$.data.resultCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(1));

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday)
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syntheticTrafficExcluded").value(false))
                .andExpect(jsonPath("$.data.syntheticIsolationLevel").value("all_traffic"))
                .andExpect(jsonPath("$.data.metricSource").value("daily_metric"))
                .andExpect(jsonPath("$.data.resultCreated").value(2))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(2));

        MvcResult defaultAggregatedListResult = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();
        JsonNode defaultAggregatedRecords = objectMapper.readTree(
                        defaultAggregatedListResult.getResponse().getContentAsString())
                .get("data")
                .get("records");
        assertTrue(containsShortCode(defaultAggregatedRecords, organicShortCode));
        assertEquals(false, containsShortCode(defaultAggregatedRecords, syntheticShortCode));

        MvcResult includeSyntheticAggregatedListResult = mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", yesterday)
                        .param("endDate", yesterday)
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        JsonNode includeSyntheticAggregatedRecords = objectMapper.readTree(
                        includeSyntheticAggregatedListResult.getResponse().getContentAsString())
                .get("data")
                .get("records");
        assertTrue(containsShortCode(includeSyntheticAggregatedRecords, organicShortCode));
        assertTrue(containsShortCode(includeSyntheticAggregatedRecords, syntheticShortCode));
    }

    @Test
    void shouldExcludeSyntheticTrafficFromDefaultShortLinkVisitDetails() throws Exception {
        JsonNode data = createValidResult("client-detail", "organic", "spring-launch");
        String shortCode = data.get("shortCode").asText();

        mockMvc.perform(get("/s/" + shortCode)
                        .param("channel", "share")
                        .param("campaign", "result-card")
                        .header("X-Client-Id", "client-organic-viewer")
                        .header("User-Agent", "Mozilla/5.0 iPhone Mobile"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/s/" + shortCode)
                        .param("channel", "perf-test")
                        .param("campaign", "frontend-live-gate")
                        .header("X-Client-Id", "client-perf-viewer")
                        .header("User-Agent", "Playwright"))
                .andExpect(status().is3xxRedirection());
        awaitShortLinkVisitCount(shortCode, 2);

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].channel").value("share"))
                .andExpect(jsonPath("$.data.records[0].campaign").value("result-card"));

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token")
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].channel").value("perf-test"))
                .andExpect(jsonPath("$.data.records[0].campaign").value("frontend-live-gate"));
    }

    @Test
    void shouldNotUseExternalAccessRecordsWhenSyntheticTrafficIsExcluded() throws Exception {
        JsonNode data = createValidResult("client-external-detail", "organic", "spring-launch");
        String shortCode = data.get("shortCode").asText();

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        verify(externalShortLinkStatsAdapter, never()).fetchAccessRecords(
                org.mockito.ArgumentMatchers.any(ShortLinkEntity.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(AdminDateRange.class));
    }

    @Test
    void shouldAllowExternalAccessRecordsWhenDetailStatSourceIsExternal() throws Exception {
        JsonNode data = createValidResult("client-external-detail-all", "organic", "spring-launch");
        String shortCode = data.get("shortCode").asText();
        when(externalShortLinkStatsAdapter.fetchAccessRecords(
                org.mockito.ArgumentMatchers.any(ShortLinkEntity.class),
                eq(1L),
                eq(20L),
                org.mockito.ArgumentMatchers.any(AdminDateRange.class)))
                .thenReturn(Optional.of(new com.wuxing.persona.vo.PageVO<>(1, 20, 7, java.util.List.of())));

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token")
                        .param("statSource", "external"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(7));
        verify(externalShortLinkStatsAdapter).fetchAccessRecords(
                org.mockito.ArgumentMatchers.any(ShortLinkEntity.class),
                eq(1L),
                eq(20L),
                org.mockito.ArgumentMatchers.any(AdminDateRange.class));
    }

    @Test
    void shouldBypassAdminOverviewCacheWhenForceRefreshIsRequested() throws Exception {
        when(valueOperations.get("admin:overview:version")).thenReturn("0");
        when(valueOperations.get(argThat((String key) -> key != null && key.startsWith("admin:overview:v0:"))))
                .thenReturn("{\"resultCreated\":99,\"shortLinkVisits\":99}");

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("includeSynthetic", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(99))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(99));

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("includeSynthetic", "true")
                        .param("forceRefresh", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(0))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(0));
    }

    @Test
    void shouldRejectInvalidAdminDateRange() throws Exception {
        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be before or equal to endDate"));
    }

    @Test
    void shouldRejectInvalidResultRequest() throws Exception {
        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 2002,
                                  "birthMonth": 13,
                                  "answers": [
                                    {"questionCode":"Q1","optionCode":"METAL"},
                                    {"questionCode":"Q2","optionCode":"WATER"},
                                    {"questionCode":"Q3","optionCode":"METAL"},
                                    {"questionCode":"Q4","optionCode":"EARTH"},
                                    {"questionCode":"Q5","optionCode":"FIRE"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectDuplicateQuestionAnswers() throws Exception {
        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 2002,
                                  "birthMonth": 8,
                                  "answers": [
                                    {"questionCode":"Q1","optionCode":"METAL"},
                                    {"questionCode":"Q1","optionCode":"WATER"},
                                    {"questionCode":"Q2","optionCode":"METAL"},
                                    {"questionCode":"Q3","optionCode":"EARTH"},
                                    {"questionCode":"Q4","optionCode":"FIRE"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("answers must contain 5 unique questions"));
    }

    @Test
    void shouldRejectImpossibleBirthDate() throws Exception {
        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 2001,
                                  "birthMonth": 2,
                                  "birthDay": 29,
                                  "answers": [
                                    {"questionCode":"Q1","optionCode":"METAL"},
                                    {"questionCode":"Q2","optionCode":"WATER"},
                                    {"questionCode":"Q3","optionCode":"METAL"},
                                    {"questionCode":"Q4","optionCode":"EARTH"},
                                    {"questionCode":"Q5","optionCode":"FIRE"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthDate must be a real calendar date"));
    }

    @Test
    void shouldCreateAndReloadDualMatchByShortCodes() throws Exception {
        JsonNode partner = createValidResult("partner-client");
        String partnerShortCode = partner.get("shortCode").asText();
        String partnerResultId = partner.get("resultId").asText();

        mockMvc.perform(get("/api/matches/candidates/" + partnerShortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortCode").value(partnerShortCode))
                .andExpect(jsonPath("$.data.resultId").value(partnerResultId))
                .andExpect(jsonPath("$.data.displayName").exists());

        MvcResult createResponse = mockMvc.perform(post("/api/matches")
                        .header("X-Client-Id", "current-client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMatchRequestBody(partnerShortCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partnerShortCode").value(partnerShortCode))
                .andExpect(jsonPath("$.data.currentShortCode").exists())
                .andExpect(jsonPath("$.data.partnerResult.resultId").value(partnerResultId))
                .andExpect(jsonPath("$.data.currentResult.resultId").exists())
                .andExpect(jsonPath("$.data.compatibilityScore").exists())
                .andExpect(jsonPath("$.data.relationLabel").exists())
                .andExpect(jsonPath("$.data.suggestions[0]").exists())
                .andReturn();
        JsonNode match = objectMapper.readTree(createResponse.getResponse().getContentAsString()).get("data");
        int score = match.get("compatibilityScore").asInt();
        assertTrue(score >= 58 && score <= 96);

        String currentShortCode = match.get("currentShortCode").asText();
        mockMvc.perform(get("/api/matches/" + partnerShortCode + "/" + currentShortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(partnerShortCode + "-" + currentShortCode))
                .andExpect(jsonPath("$.data.partnerShortCode").value(partnerShortCode))
                .andExpect(jsonPath("$.data.currentShortCode").value(currentShortCode));
    }

    @Test
    void shouldRejectInvalidMatchShortCode() throws Exception {
        mockMvc.perform(get("/api/matches/candidates/abc123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("shortCode not found"));

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMatchRequestBody("bad-code")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("shortCode must be base62 and length 6 or 7"));
    }

    @Test
    void shouldRejectInvalidEventType() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("X-Client-Id", "client-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "UNKNOWN_EVENT",
                                  "pagePath": "/"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("eventType is invalid"));
    }

    @ParameterizedTest
    @MethodSource("adminRequestsWithoutToken")
    void shouldRejectAllAdminEndpointsWithoutToken(RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private static Stream<RequestBuilder> adminRequestsWithoutToken() {
        return Stream.of(
                get("/api/admin/overview"),
                get("/api/admin/short-links"),
                get("/api/admin/short-links/export"),
                get("/api/admin/short-links/abc123/visits"),
                get("/api/admin/external-shortlink/status"),
                get("/api/admin/visit-events/runtime"),
                post("/api/admin/analytics/aggregate")
        );
    }

    @Test
    void shouldReturnExternalShortLinkRuntimeStatusAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/external-shortlink/status")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.data.mode").value("internal"))
                .andExpect(jsonPath("$.data.externalMode").value(false))
                .andExpect(jsonPath("$.data.message").value("probe skipped"));
    }

    @Test
    void shouldRedirectUnknownShortCodeToNotFound() throws Exception {
        mockMvc.perform(get("/s/abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/not-found"));
    }

    @Test
    void shouldRejectMalformedShortCode() throws Exception {
        mockMvc.perform(get("/s/bad-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("shortCode must be base62 and length 6 or 7"));

        mockMvc.perform(get("/api/admin/short-links/bad-code/visits")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("shortCode must be base62 and length 6 or 7"));
    }

    private JsonNode createValidResult(String clientId) throws Exception {
        return createValidResult(clientId, "organic", "spring-launch");
    }

    private JsonNode createValidResult(String clientId, String channel, String campaign) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/results")
                        .header("X-Client-Id", clientId)
                        .header("X-Session-Id", "session-a")
                        .header("X-Channel", channel)
                        .header("X-Campaign", campaign)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validResultRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.resultId").exists())
                .andExpect(jsonPath("$.data.shortCode").exists())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
    }

    private void recordEventApiSmoke() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("X-Client-Id", "client-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "SHARED_RESULT_CTA_CLICK",
                                  "pagePath": "/api-event-smoke",
                                  "sessionId": "session-a",
                                  "channel": "api-smoke",
                                  "campaign": "integration-test"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private void seedGrowthEvent(String eventType, String pagePath) {
        jdbcTemplate.update("""
                        INSERT INTO visit_event (
                            event_type, page_path, client_id_hash, session_id_hash, ip_hash, user_agent_hash,
                            channel, campaign, device_type, event_date, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                eventType,
                pagePath,
                "client-a-hash",
                "session-a-hash",
                "ip-hash",
                "ua-hash",
                "organic",
                "spring-launch",
                "desktop",
                Date.valueOf(LocalDate.now()),
                Timestamp.valueOf(LocalDateTime.now()));
    }

    private void seedLocalShortLinks(int count) {
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            String resultId = String.format("bulk-result-%03d", i);
            String shortCode = String.format("B%05d", i);
            Timestamp timestamp = Timestamp.valueOf(baseTime.minusSeconds(i));
            jdbcTemplate.update("""
                            INSERT INTO user_result (
                                result_id, birth_year, birth_month, birth_day, birth_time_range,
                                answer_json, primary_element, secondary_element, primary_percent, secondary_percent,
                                all_element_scores_json, star_officer_code, star_officer_name, keywords_json,
                                layout_explanation, strength_text, relationship_text, status, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    resultId,
                    2002,
                    8,
                    null,
                    null,
                    "[]",
                    "FIRE",
                    "EARTH",
                    60,
                    40,
                    "{}",
                    "bright-fire",
                    "明火",
                    "[]",
                    "bulk layout",
                    "bulk strength",
                    "bulk relationship",
                    1,
                    timestamp,
                    timestamp);
            jdbcTemplate.update("""
                            INSERT INTO short_link (
                                short_code, result_id, original_path, short_url, pv_count, uv_count, uip_count,
                                last_visit_at, status, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    shortCode,
                    resultId,
                    "/result/" + resultId,
                    "http://localhost:8080/s/" + shortCode,
                    0,
                    0,
                    0,
                    null,
                    1,
                    timestamp,
                    timestamp);
        }
    }

    private void awaitShortLinkVisitCount(String shortCode, long expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ASYNC_EVENT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM visit_event WHERE event_type = 'SHORT_LINK_VISIT' AND short_code = ?",
                    Long.class,
                    shortCode
            );
            if (count != null && count >= expectedCount) {
                return;
            }
            Thread.sleep(25);
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visit_event WHERE event_type = 'SHORT_LINK_VISIT' AND short_code = ?",
                Long.class,
                shortCode
        );
        assertEquals(expectedCount, count == null ? 0 : count);
    }

    private boolean containsName(JsonNode records, String name) {
        if (records == null || !records.isArray()) {
            return false;
        }
        for (JsonNode record : records) {
            if (name.equals(record.path("name").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsShortCode(JsonNode records, String shortCode) {
        if (records == null || !records.isArray()) {
            return false;
        }
        for (JsonNode record : records) {
            if (shortCode.equals(record.path("shortCode").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findShortLinkItem(JsonNode records, String shortCode) {
        for (JsonNode record : records) {
            if (shortCode.equals(record.get("shortCode").asText())) {
                return record;
            }
        }
        return null;
    }

    private void moveAllRecordsToDate(LocalDate date) {
        Timestamp timestamp = Timestamp.valueOf(date.atTime(10, 0));
        jdbcTemplate.update("UPDATE visit_event SET created_at = ?, event_date = ?", timestamp, Date.valueOf(date));
        jdbcTemplate.update("UPDATE user_result SET created_at = ?, updated_at = ?", timestamp, timestamp);
        jdbcTemplate.update("UPDATE short_link SET created_at = ?, updated_at = ?, last_visit_at = ?",
                timestamp, timestamp, timestamp);
    }

    private String validResultRequestBody() {
        return """
                {
                  "birthYear": 2002,
                  "birthMonth": 8,
                  "birthDay": null,
                  "birthTimeRange": null,
                  "answers": [
                    {"questionCode":"Q1","optionCode":"METAL"},
                    {"questionCode":"Q2","optionCode":"WATER"},
                    {"questionCode":"Q3","optionCode":"METAL"},
                    {"questionCode":"Q4","optionCode":"EARTH"},
                    {"questionCode":"Q5","optionCode":"FIRE"}
                  ]
                }
                """;
    }

    private String validMatchRequestBody(String partnerShortCode) {
        return """
                {
                  "partnerShortCode": "%s",
                  "birthYear": 2005,
                  "birthMonth": 3,
                  "birthDay": null,
                  "birthTimeRange": null,
                  "answers": [
                    {"questionCode":"Q1","optionCode":"WOOD"},
                    {"questionCode":"Q2","optionCode":"FIRE"},
                    {"questionCode":"Q3","optionCode":"WOOD"},
                    {"questionCode":"Q4","optionCode":"EARTH"},
                    {"questionCode":"Q5","optionCode":"WATER"}
                  ]
                }
                """.formatted(partnerShortCode);
    }
}
