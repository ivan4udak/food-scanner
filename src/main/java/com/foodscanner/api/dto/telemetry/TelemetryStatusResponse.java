package com.foodscanner.api.dto.telemetry;

/**
 * Слой: api (DTO).
 * Универсальный {"status":"OK"} для session/activity.
 */
public record TelemetryStatusResponse(String status) {
    public static final TelemetryStatusResponse OK = new TelemetryStatusResponse("OK");
}
