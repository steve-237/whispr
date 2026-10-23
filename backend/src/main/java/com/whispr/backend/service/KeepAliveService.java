package com.whispr.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "whispr.keep-alive.enabled", havingValue = "true")
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whispr.keep-alive.url:}")
    private String keepAliveUrl;

    // S'exécute toutes les 10 minutes (600000 ms)
    @Scheduled(fixedRate = 600000)
    public void pingSelf() {
        if (keepAliveUrl == null || keepAliveUrl.isBlank()) {
            return;
        }
        try {
            String response = restTemplate.getForObject(keepAliveUrl, String.class);
            logger.info("Keep-alive ping sent to self: {}", response);
        } catch (Exception e) {
            logger.error("Failed to ping self for keep-alive", e);
        }
    }
}
