package com.whispr.backend.dto.admin;

public record CreateAdRequest(
        String title,
        String description,
        String imageUrl,
        String targetUrl,
        String ctaText,
        String badgeText
) {}
