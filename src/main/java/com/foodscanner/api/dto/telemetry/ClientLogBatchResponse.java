package com.foodscanner.api.dto.telemetry;

/**
 * Слой: api (DTO).
 * Ответ на приём партии логов.
 */
public record ClientLogBatchResponse(String status, int accepted) {
    public static ClientLogBatchResponse ok(int accepted) {
        return new ClientLogBatchResponse("OK", accepted);
    }
}
