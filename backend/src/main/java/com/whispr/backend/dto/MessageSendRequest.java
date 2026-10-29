package com.whispr.backend.dto;

public record MessageSendRequest(
        String content,
        String type,
        String audioUrl,
        Integer audioDurationSeconds,
        String voiceFilter
) {
    public MessageSendRequest(String content, String type) {
        this(content, type, null, null, null);
    }
}
