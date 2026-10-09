package com.whispr.backend.dto;

import java.util.Map;

public record VisitorAnalyticsDto(
        long totalViews,
        long totalMessages,
        Map<String, Long> sources,
        Map<String, Long> topCities,
        Map<String, Long> topCountries
) {
}
