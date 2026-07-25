package com.whispr.backend.dto;

import java.util.UUID;

public record LinkDto(
        UUID id,
        String slug,
        Boolean isActive,
        String profileBio,
        String profileAvatarUrl,
        String profileThemeId,
        String profileDailyQuestion
) {
}
