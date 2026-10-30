package com.whispr.backend.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record MessageDto(
        UUID id,
        String content,
        String type,
        String status,
        ZonedDateTime createdAt,
        String country,
        String deviceHint,
        @com.fasterxml.jackson.annotation.JsonProperty("isRead") boolean isRead,
        String aiCategory,
        String audioUrl,
        Integer audioDurationSeconds,
        String voiceFilter,
        BigDecimal tipAmountEur,
        @com.fasterxml.jackson.annotation.JsonProperty("isTipPaid") boolean isTipPaid
) {
    public MessageDto(
            UUID id,
            String content,
            String type,
            String status,
            ZonedDateTime createdAt,
            String country,
            String deviceHint,
            boolean isRead,
            String aiCategory
    ) {
        this(id, content, type, status, createdAt, country, deviceHint, isRead, aiCategory, null, null, null, null, false);
    }

    public MessageDto(
            UUID id,
            String content,
            String type,
            String status,
            ZonedDateTime createdAt,
            String country,
            String deviceHint,
            boolean isRead,
            String aiCategory,
            String audioUrl,
            Integer audioDurationSeconds,
            String voiceFilter
    ) {
        this(id, content, type, status, createdAt, country, deviceHint, isRead, aiCategory, audioUrl, audioDurationSeconds, voiceFilter, null, false);
    }
}
