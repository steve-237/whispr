package com.whispr.backend.dto;

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
        String voiceFilter
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
        this(id, content, type, status, createdAt, country, deviceHint, isRead, aiCategory, null, null, null);
    }
}
