package com.foodscanner.application.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (команда).
 * Снимок клиентской сессии для upsert по sessionId.
 */
public record RecordClientSessionCommand(
        UUID contributorId,
        UUID sessionId,
        Instant receivedAt,
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
