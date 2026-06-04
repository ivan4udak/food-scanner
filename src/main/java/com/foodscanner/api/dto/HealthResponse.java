package com.foodscanner.api.dto;

import java.time.Instant;

/**
 * Слой: api
 * Ответ GET /api/v1/health — состояние backend и хранилища (MinIO).
 * status = OK, если всё поднято; DEGRADED, если хранилище недоступно.
 */
public record HealthResponse(String status, String backend, String storage, Instant timestamp) {

    public static HealthResponse from(boolean storageUp) {
        return new HealthResponse(
            storageUp ? "OK" : "DEGRADED",
            "UP",
            storageUp ? "UP" : "DOWN",
            Instant.now());
    }
}
