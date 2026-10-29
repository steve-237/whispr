package com.whispr.backend.dto;

import java.math.BigDecimal;

public record CreatePayoutRequest(
    BigDecimal amountEur,
    String method,
    String payoutDetails
) {}
