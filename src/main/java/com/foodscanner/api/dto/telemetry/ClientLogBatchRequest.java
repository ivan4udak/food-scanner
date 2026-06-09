package com.foodscanner.api.dto.telemetry;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Слой: api (DTO).
 * POST /api/v1/client-logs/batch
 */
public record ClientLogBatchRequest(
        @NotNull UUID sessionId,
        String clientVersion,
        String pwaVersion,
        @NotNull List<ClientLogDto> logs
) {}
