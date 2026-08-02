package com.whispr.backend.dto;

public record StatsDto(
        long totalMessages,
        long positiveCount,
        long neutralCount,
        long negativeCount,
        String aiSummary
) {
}
