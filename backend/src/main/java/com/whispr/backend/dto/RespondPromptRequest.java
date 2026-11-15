package com.whispr.backend.dto;

import java.util.UUID;

public record RespondPromptRequest(
        UUID selectedOptionId,
        Integer ratingValue,
        String textAnswer
) {
}
