package com.whispr.backend.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record WalletDto(
        int coins,
        BigDecimal totalSpentEur,
        boolean isPro,
        ZonedDateTime proExpiresAt,
        BigDecimal earningsEur,
        int referralCount,
        String affiliateCode,
        String stripeSubscriptionId,
        String subscriptionStatus
) {
    public WalletDto(int coins, BigDecimal totalSpentEur, boolean isPro, ZonedDateTime proExpiresAt, BigDecimal earningsEur, int referralCount, String affiliateCode) {
        this(coins, totalSpentEur, isPro, proExpiresAt, earningsEur, referralCount, affiliateCode, null, null);
    }
}
