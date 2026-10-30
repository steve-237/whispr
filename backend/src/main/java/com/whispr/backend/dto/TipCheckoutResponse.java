package com.whispr.backend.dto;

import java.util.UUID;

public record TipCheckoutResponse(
        String sessionId,
        String checkoutUrl,
        UUID messageId
) {}
