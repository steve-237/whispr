package com.whispr.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    
    // S'exécute toutes les 10 minutes (600000 ms)
    @Scheduled(fixedRate = 600000)
    public void pingSelf() {
        try {
            String url = "https://whispr-z6zx.onrender.com/api/health";
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Keep-alive ping sent to self: {}", response);
        } catch (Exception e) {
            logger.error("Failed to ping self for keep-alive", e);
        }
    }
}
