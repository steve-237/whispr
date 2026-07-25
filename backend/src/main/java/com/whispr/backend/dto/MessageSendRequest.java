package com.whispr.backend.dto;

public record MessageSendRequest(
        String content,
        String type
) {
}
