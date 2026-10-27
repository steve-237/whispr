package com.whispr.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private final Environment environment;

    public DataSourceConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        String rawUrl = getFirstConfigured("spring.datasource.url", "DATABASE_URL", "SPRING_DATASOURCE_URL");
        String user = getFirstConfigured("spring.datasource.username", "DATABASE_USER", "SPRING_DATASOURCE_USERNAME");
        String pass = getFirstConfigured("spring.datasource.password", "DATABASE_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
        String driverClassName = getFirstConfigured("spring.datasource.driver-class-name", "SPRING_DATASOURCE_DRIVER_CLASS_NAME");

        String jdbcUrl = rawUrl != null ? rawUrl.trim() : "";

        // Fast-path for H2 in-memory test database
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user != null ? user : "sa");
            config.setPassword(pass != null ? pass : "");
            config.setDriverClassName("org.h2.Driver");
            return new HikariDataSource(config);
        }

        // Support direct cloud URLs formatted as postgres:// or postgresql://
        if (!jdbcUrl.isEmpty() && (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://"))) {
            try {
                String withoutScheme = jdbcUrl.replaceFirst("^postgres(ql)?://", "");
                
                int slashIndex = withoutScheme.indexOf('/');
                int atIndex = slashIndex != -1 ? withoutScheme.substring(0, slashIndex).lastIndexOf('@') : withoutScheme.lastIndexOf('@');

                String hostPortDb = withoutScheme;
                if (atIndex != -1) {
                    String userInfo = withoutScheme.substring(0, atIndex);
                    hostPortDb = withoutScheme.substring(atIndex + 1);

                    int colonIndex = userInfo.indexOf(':');
                    if (colonIndex != -1) {
                        if (user == null || user.isEmpty()) {
                            user = java.net.URLDecoder.decode(userInfo.substring(0, colonIndex), java.nio.charset.StandardCharsets.UTF_8);
                        }
                        if (pass == null || pass.isEmpty()) {
                            pass = java.net.URLDecoder.decode(userInfo.substring(colonIndex + 1), java.nio.charset.StandardCharsets.UTF_8);
                        }
                    } else if (user == null || user.isEmpty()) {
                        user = java.net.URLDecoder.decode(userInfo, java.nio.charset.StandardCharsets.UTF_8);
                    }
                }

                String query = "";
                int qIndex = hostPortDb.indexOf('?');
                if (qIndex != -1) {
                    query = hostPortDb.substring(qIndex + 1);
                    hostPortDb = hostPortDb.substring(0, qIndex);
                }

                // If default port is omitted (e.g. host/database)
                if (!hostPortDb.contains(":") && hostPortDb.contains("/")) {
                    int firstSlash = hostPortDb.indexOf('/');
                    String hostOnly = hostPortDb.substring(0, firstSlash);
                    String dbOnly = hostPortDb.substring(firstSlash);
                    hostPortDb = hostOnly + ":5432" + dbOnly;
                }

                jdbcUrl = "jdbc:postgresql://" + hostPortDb;
                if (!query.isBlank()) {
                    jdbcUrl += "?" + query;
                }

                log.info("Converted cloud URI to PostgreSQL JDBC URL: {}", sanitizeUrl(jdbcUrl));
            } catch (Exception e) {
                log.warn("Could not parse cloud PostgreSQL URI, keeping raw URL: {}", e.getMessage());
            }
        }

        // Supabase / Transaction Pooler optimization:
        // Port 6543 uses transaction-level pooling (PgBouncer/Supavisor).
        // Setting prepareThreshold=0 prevents "prepared statement already exists" errors in PostgreSQL JDBC.
        if (jdbcUrl.contains(":6543") && !jdbcUrl.contains("prepareThreshold=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "prepareThreshold=0";
            log.info("Detected transaction pooler (port 6543): enabled prepareThreshold=0 for Supabase compatibility");
        }

        // Supabase strictly enforces SSL
        if ((jdbcUrl.contains("supabase.co") || jdbcUrl.contains("supabase.com")) 
                && !jdbcUrl.contains("sslmode=") 
                && !jdbcUrl.contains("ssl=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        // Apply sslmode=prefer for general remote postgres URLs if no sslmode is specified.
        if (jdbcUrl.startsWith("jdbc:postgresql://") 
                && !jdbcUrl.contains("localhost") 
                && !jdbcUrl.contains("127.0.0.1") 
                && !jdbcUrl.contains("sslmode=")
                && !jdbcUrl.contains("ssl=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=prefer";
        }

        config.setJdbcUrl(jdbcUrl);
        if (user != null && !user.isEmpty()) {
            config.setUsername(user);
        }
        if (pass != null && !pass.isEmpty()) {
            config.setPassword(pass);
        }
        if (driverClassName != null && !driverClassName.isBlank()) {
            config.setDriverClassName(driverClassName);
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        }

        // Connection pool resilience against hung connections
        config.setConnectionTimeout(15000); // 15 seconds
        config.setValidationTimeout(5000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);

        return new HikariDataSource(config);
    }

    private String getFirstConfigured(String... propertyNames) {
        for (String prop : propertyNames) {
            String val = environment.getProperty(prop);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }

    private String sanitizeUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@/]+@", ":***@");
    }
}
