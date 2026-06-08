package com.foodscanner.api.dto.telemetry;

import java.util.Map;

/**
 * Слой: api (DTO).
 * Одна строка клиентского лога во входящей партии. UUID/время — строками
 * (толерантный парсинг в маппере, плохие значения → null).
 */
public record ClientLogDto(
        String id,
        String timestamp,
        String level,
        String category,
        String event,
        String message,
        String screen,
        Map<String, Object> metadata,
        Long durationMs,
        String stackTrace,
        String correlationId,
        String requestId,
        String barcode,
        String draftId,
        String catalogEntryId,
        String photoId,
        String apiMethod,
        String apiPath,
        Integer httpStatus
) {}
