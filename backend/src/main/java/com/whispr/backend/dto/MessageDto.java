package com.whispr.backend.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record MessageDto(
        UUID id,
        String content,
        String type,
        String status,
        ZonedDateTime createdAt
) {
}
