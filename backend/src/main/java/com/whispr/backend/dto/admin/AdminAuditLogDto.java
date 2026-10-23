package com.whispr.backend.dto.admin;

public record AdminAuditLogDto(
        String targetUser,
        String hashedIp,
        String rawIp,
        String userAgent,
        String country,
        String createdAt
) {}
