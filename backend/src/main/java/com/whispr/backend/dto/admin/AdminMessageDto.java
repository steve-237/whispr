package com.whispr.backend.dto.admin;

public record AdminMessageDto(
        String id,
        String content,
        String targetUser,
        Boolean isToxic,
        String createdAt
) {}
