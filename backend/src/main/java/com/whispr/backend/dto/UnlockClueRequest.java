package com.whispr.backend.dto;

import java.util.UUID;

public record UnlockClueRequest(
        UUID messageId,
        String clueType // 'LOCATION', 'DEVICE', 'NETWORK'
) {
}
