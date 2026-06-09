package com.wuxing.persona;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:wuxing;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema-h2.sql",
        "app.base-url=http://localhost:8080",
        "app.admin-token=test-token",
        "app.hash-salt=test-salt"
})
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM visit_event",
        "DELETE FROM short_link",
        "DELETE FROM user_result"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MvpFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUpRedisMock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void shouldCompleteResultShortLinkAndAdminFlow() throws Exception {
        JsonNode data = createValidResult("client-a");
        String resultId = data.get("resultId").asText();
        String shortCode = data.get("shortCode").asText();

        mockMvc.perform(get("/api/results/" + resultId).header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultId").value(resultId));

        mockMvc.perform(get("/s/" + shortCode).header("User-Agent", "JUnit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/result/" + resultId + "?sc=" + shortCode));

        mockMvc.perform(get("/api/admin/overview").header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(1))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(1));
    }

    @Test
    void shouldReturnShortLinkListAndVisitDetailStats() throws Exception {
        JsonNode data = createValidResult("client-a");
        String shortCode = data.get("shortCode").asText();

        mockMvc.perform(get("/s/" + shortCode).header("X-Client-Id", "same-client"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/s/" + shortCode).header("X-Client-Id", "same-client"))
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
                .andExpect(jsonPath("$.data.records[0].uip").value(1));

        mockMvc.perform(get("/api/admin/short-links/" + shortCode + "/visits")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].eventType").value("SHORT_LINK_VISIT"))
                .andExpect(jsonPath("$.data.records[0].clientIdHash").exists())
                .andExpect(jsonPath("$.data.records[0].ipHash").exists());
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
                .andExpect(jsonPath("$.data.shortLinkVisits").value(1));

        mockMvc.perform(get("/api/admin/overview")
                        .header("X-Admin-Token", "test-token")
                        .param("startDate", future)
                        .param("endDate", future))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCreated").value(0))
                .andExpect(jsonPath("$.data.shortLinkCreated").value(0))
                .andExpect(jsonPath("$.data.shortLinkVisits").value(0));

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

    @Test
    void shouldProtectAdminApisWithToken() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
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
    }

    private JsonNode createValidResult(String clientId) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/results")
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validResultRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.resultId").exists())
                .andExpect(jsonPath("$.data.shortCode").exists())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
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
}
