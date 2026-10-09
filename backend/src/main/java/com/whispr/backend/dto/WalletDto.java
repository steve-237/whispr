package com.whispr.backend.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record WalletDto(
        int coins,
        BigDecimal totalSpentEur,
        boolean isPro,
        ZonedDateTime proExpiresAt
) {
}
