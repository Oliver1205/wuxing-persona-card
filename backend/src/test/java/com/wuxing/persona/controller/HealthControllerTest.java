package com.wuxing.persona.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wuxing.persona.common.ApiResponse;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerTest {

    @Test
    void readinessShouldReturnUpWhenCoreTablesExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        HealthController controller = new HealthController(jdbcTemplate);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.readiness();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
        assertEquals("UP", response.getBody().getData().get("status"));
        assertEquals("core_schema", response.getBody().getData().get("scope"));
        assertTrue(coreTables(response).values().stream().allMatch("ok"::equals));
    }

    @Test
    void readinessShouldReturnServiceUnavailableWhenCoreTableIsMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("user_result")) {
                throw new BadSqlGrammarException("readiness", sql, new SQLException("table missing"));
            }
            return 0L;
        });
        HealthController controller = new HealthController(jdbcTemplate);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getCode());
        assertEquals("DOWN", response.getBody().getData().get("status"));
        assertEquals("core_schema", response.getBody().getData().get("scope"));
        assertEquals("unavailable", coreTables(response).get("user_result"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> coreTables(ResponseEntity<ApiResponse<Map<String, Object>>> response) {
        assertNotNull(response.getBody());
        return (Map<String, String>) response.getBody().getData().get("coreTables");
    }
}
