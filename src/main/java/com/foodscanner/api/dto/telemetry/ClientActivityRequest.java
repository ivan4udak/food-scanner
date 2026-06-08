package com.foodscanner.api.dto.telemetry;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Слой: api (DTO).
 * POST /api/v1/client/activity
 */
public record ClientActivityRequest(
        @NotNull UUID sessionId,
        String screen,
        Boolean online,
        String timestamp
) {}
