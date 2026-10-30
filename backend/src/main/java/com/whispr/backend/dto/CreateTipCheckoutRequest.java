package com.whispr.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTipCheckoutRequest(
        @NotBlank String slug,
        @NotBlank String content,
        String type,
        @NotNull @DecimalMin(value = "0.50", message = "Le pourboire minimum est de 0.50 €") BigDecimal tipAmountEur,
        String audioUrl,
        Integer audioDurationSeconds,
        String voiceFilter
) {}
