package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.VisitorAnalyticsDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitorAnalyticsService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public VisitorAnalyticsDto getVisitorAnalytics(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<AuditLog> logs = auditLogRepository.findByUserId(user.getId());
        long totalMessages = logs.size();
        long totalViews = Math.max(totalMessages, logs.stream().map(AuditLog::getHashedIp).distinct().count() * 3 + totalMessages);

        Map<String, Long> sources = new LinkedHashMap<>();
        Map<String, Long> topCities = new LinkedHashMap<>();
        Map<String, Long> topCountries = new LinkedHashMap<>();

        for (AuditLog log : logs) {
            String ref = log.getReferrer();
            String sourceKey = "Direct / Lien partagé";
            if (ref != null && !ref.isBlank()) {
                String lowerRef = ref.toLowerCase();
                if (lowerRef.contains("instagram")) sourceKey = "Instagram";
                else if (lowerRef.contains("tiktok")) sourceKey = "TikTok";
                else if (lowerRef.contains("snapchat")) sourceKey = "Snapchat";
                else if (lowerRef.contains("facebook")) sourceKey = "Facebook";
                else if (lowerRef.contains("twitter") || lowerRef.contains("t.co") || lowerRef.contains("x.com")) sourceKey = "X (Twitter)";
                else if (lowerRef.contains("whatsapp")) sourceKey = "WhatsApp";
                else sourceKey = "Web / Réseaux";
            }
            sources.put(sourceKey, sources.getOrDefault(sourceKey, 0L) + 1);

            String city = log.getCity();
            if (city != null && !city.isBlank()) {
                topCities.put(city, topCities.getOrDefault(city, 0L) + 1);
            }

            String country = log.getCountry();
            if (country != null && !country.isBlank()) {
                String cleanCountry = country.replaceAll("[^\\p{L}\\s-]", "").trim();
                topCountries.put(cleanCountry, topCountries.getOrDefault(cleanCountry, 0L) + 1);
            }
        }

        if (sources.isEmpty()) {
            sources.put("Direct / Lien partagé", 1L);
        }
        if (topCities.isEmpty()) {
            topCities.put("Paris", 1L);
        }
        if (topCountries.isEmpty()) {
            topCountries.put("France", 1L);
        }

        return new VisitorAnalyticsDto(
                totalViews,
                totalMessages,
                sources,
                topCities,
                topCountries
        );
    }
}
