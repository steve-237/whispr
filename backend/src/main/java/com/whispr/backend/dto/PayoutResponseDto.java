package com.whispr.backend.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PayoutResponseDto(
    UUID id,
    UUID userId,
    String userPseudo,
    String userEmail,
    BigDecimal amountEur,
    String method,
    String payoutDetails,
    String status,
    String adminNote,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {}
