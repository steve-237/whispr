package com.whispr.backend.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record AdvertisementDto(
        UUID id,
        String title,
        String description,
        String imageUrl,
        String targetUrl,
        String ctaText,
        String badgeText,
        boolean isActive,
        int clicksCount,
        int viewsCount,
        ZonedDateTime createdAt
) {
}
