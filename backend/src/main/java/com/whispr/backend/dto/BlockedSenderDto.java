package com.whispr.backend.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record BlockedSenderDto(
        UUID id,
        String displayName,
        String reason,
        ZonedDateTime createdAt
) {}
