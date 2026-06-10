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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql",
        "app.base-url=http://localhost:8080",
        "app.admin-token=test-token",
        "app.hash-salt=test-salt"
})
@AutoConfigureMockMvc
class MvpMySqlContainerIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("wuxing_persona")
            .withUsername("wuxing")
            .withPassword("wuxing");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @BeforeEach
    void setUpRedisMock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void shouldRunMvpFlowAgainstMysqlSchema() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/results")
                        .header("X-Client-Id", "mysql-it-client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultId").exists())
                .andExpect(jsonPath("$.data.shortCode").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        String resultId = data.get("resultId").asText();
        String shortCode = data.get("shortCode").asText();

        mockMvc.perform(get("/s/" + shortCode).header("X-Client-Id", "mysql-it-client"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/result/" + resultId + "?sc=" + shortCode));

        mockMvc.perform(get("/api/admin/short-links")
                        .header("X-Admin-Token", "test-token")
                        .param("keyword", resultId)
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].shortCode").value(shortCode));
    }
}
