package com.whispr.backend.dto;

public record CheckoutSessionResponse(
    String sessionId,
    String checkoutUrl
) {}
