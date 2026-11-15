package com.whispr.backend.dto;

import java.util.List;

public record CreatePromptRequest(
        String question,
        String type,
        Integer scaleMax,
        List<String> options
) {
}
