package com.whispr.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutSessionRequest(
    @NotBlank String packId,
    String promoCode
) {}
