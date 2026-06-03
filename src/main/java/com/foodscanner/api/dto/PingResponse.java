package com.foodscanner.api.dto;

import java.time.Instant;

public record PingResponse(String status, Instant timestamp) {
    public static PingResponse now() {
        return new PingResponse("OK", Instant.now());
    }
}
