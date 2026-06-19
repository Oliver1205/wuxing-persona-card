package com.wuxing.persona.controller;

import com.wuxing.persona.common.ApiResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final List<String> CORE_TABLES = List.of(
            "user_result",
            "short_link",
            "visit_event",
            "site_daily_metric",
            "short_link_daily_metric");

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "wuxing-persona-backend",
                "time", OffsetDateTime.now().toString()));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, String> tableChecks = new LinkedHashMap<>();
        boolean ready = true;
        for (String table : CORE_TABLES) {
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE 1 = 0", Long.class);
                tableChecks.put(table, "ok");
            } catch (DataAccessException ex) {
                ready = false;
                tableChecks.put(table, "unavailable");
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", ready ? "UP" : "DOWN");
        data.put("scope", "core_schema");
        data.put("service", "wuxing-persona-backend");
        data.put("time", OffsetDateTime.now().toString());
        data.put("coreTables", tableChecks);

        if (ready) {
            return ResponseEntity.ok(ApiResponse.success(data));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(503, "readiness check failed", data));
    }
}
