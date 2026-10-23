package com.whispr.backend.dto.admin;

public record AdminUserDto(
        String email,
        String pseudo,
        String role,
        String createdAt
) {}
