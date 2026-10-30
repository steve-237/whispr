package com.whispr.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSuperWhisprCheckoutRequest(
        @NotBlank String slug,
        String content,
        String audioUrl,
        Integer audioDurationSeconds,
        String voiceFilter
) {}
