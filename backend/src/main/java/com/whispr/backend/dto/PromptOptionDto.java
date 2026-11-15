package com.whispr.backend.dto;

import java.util.UUID;

public record PromptOptionDto(
        UUID id,
        String optionText,
        Integer voteCount,
        Integer displayOrder,
        Double percentage
) {
}
