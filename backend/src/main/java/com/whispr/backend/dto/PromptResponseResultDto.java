package com.whispr.backend.dto;

public record PromptResponseResultDto(
        boolean success,
        String message,
        PromptDto prompt
) {
}
