package com.whispr.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateExpressClueCheckoutRequest(
        @NotNull(message = "messageId est obligatoire") UUID messageId
) {}
