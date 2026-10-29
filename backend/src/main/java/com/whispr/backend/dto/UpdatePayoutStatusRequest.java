package com.whispr.backend.dto;

public record UpdatePayoutStatusRequest(
    String status,
    String adminNote
) {}
