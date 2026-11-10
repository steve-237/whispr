package com.whispr.backend.dto;

import com.whispr.backend.domain.PromptType;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record PromptDto(
        UUID id,
        String question,
        PromptType type,
        Boolean isActive,
        Integer scaleMax,
        Integer totalResponses,
        Double averageRating,
        Integer ratingCount,
        List<PromptOptionDto> options,
        ZonedDateTime createdAt
) {
}
