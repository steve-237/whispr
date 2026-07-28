package com.whispr.backend.dto;

public record ProfileUpdateRequest(
        String bio,
        String dailyQuestion,
        String themeId,
        Boolean isActive
) {
}
