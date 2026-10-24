package com.whispr.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        String jdbcUrl = rawUrl != null ? rawUrl.trim() : "";
        String user = username != null ? username.trim() : "";
        String pass = password != null ? password.trim() : "";

        // Support direct cloud URLs formatted as postgres:// or postgresql://
        if (!jdbcUrl.isEmpty() && (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://"))) {
            try {
                String cleanUrl = jdbcUrl.replace("postgres://", "http://").replace("postgresql://", "http://");
                URI uri = new URI(cleanUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                    String[] userInfoParts = uri.getUserInfo().split(":", 2);
                    if (user.isEmpty()) {
                        user = userInfoParts[0];
                    }
                    if (pass.isEmpty() && userInfoParts.length > 1) {
                        pass = userInfoParts[1];
                    }
                }

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                    if (!jdbcUrl.contains("sslmode=")) {
                        jdbcUrl += "&sslmode=require";
                    }
                } else {
                    jdbcUrl += "?sslmode=require";
                }

                log.info("Successfully converted cloud URI to PostgreSQL JDBC URL format (host={}, port={}, database={})", host, port, path);
            } catch (Exception e) {
                log.warn("Could not parse cloud PostgreSQL URI, keeping raw URL: {}", e.getMessage());
            }
        }

        // Auto-append sslmode=require for non-localhost postgres URLs if missing
        if (jdbcUrl.startsWith("jdbc:postgresql://") && !jdbcUrl.contains("localhost") && !jdbcUrl.contains("127.0.0.1") && !jdbcUrl.contains("sslmode=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        config.setJdbcUrl(jdbcUrl);
        if (!user.isEmpty()) {
            config.setUsername(user);
        }
        if (!pass.isEmpty()) {
            config.setPassword(pass);
        }
        if (driverClassName != null && !driverClassName.isBlank()) {
            config.setDriverClassName(driverClassName);
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        }

        // Connection pool resilience against hung cloud connections
        config.setConnectionTimeout(15000); // 15 seconds max instead of 30s+
        config.setValidationTimeout(5000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);

        return new HikariDataSource(config);
    }
}
