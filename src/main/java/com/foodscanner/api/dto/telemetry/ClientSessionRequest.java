package com.foodscanner.api.dto.telemetry;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Слой: api (DTO).
 * POST /api/v1/client/session
 */
public record ClientSessionRequest(
        @NotNull UUID sessionId,
        String clientVersion,
        String pwaVersion,
        String browser,
        String os,
        String deviceType,
        String language,
        String timezone,
        Integer screenWidth,
        Integer screenHeight,
        Integer hardwareConcurrency,
        Double deviceMemory,
        String networkStatus,
        Boolean standalone
) {}
