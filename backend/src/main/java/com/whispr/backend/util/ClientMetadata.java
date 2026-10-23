package com.whispr.backend.util;

public record ClientMetadata(
        String rawIp,
        String hashedIp,
        String userAgent,
        String country,
        String city,
        String referrer
) {}
