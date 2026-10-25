package com.whispr.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "Whispr Backend is awake!");

        try (Connection conn = dataSource.getConnection()) {
            status.put("database", "CONNECTED");
            status.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            status.put("databaseUrl", sanitizeJdbcUrl(conn.getMetaData().getURL()));
        } catch (Exception e) {
            log.error("Database health check probe failed: {}", e.getMessage());
            status.put("database", "DISCONNECTED");
            status.put("databaseError", e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                status.put("databaseCause", e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }

        return ResponseEntity.ok(status);
    }

    private String sanitizeJdbcUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@/]+@", ":***@");
    }
}
