package com.whispr.backend.dto;

public record ClueResponseDto(
        String clueType,
        String clueValue,
        boolean isUnlocked
) {
}
