package com.whispr.backend.dto;

import java.util.UUID;

public record SuperWhisprCheckoutResponse(
        String sessionId,
        String checkoutUrl,
        UUID messageId
) {}
