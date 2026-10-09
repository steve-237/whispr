package com.whispr.backend.dto;

public record CheckoutSimulationRequest(
        String packId // 'COINS_50', 'COINS_150', 'COINS_500', 'PRO_MONTHLY'
) {
}
