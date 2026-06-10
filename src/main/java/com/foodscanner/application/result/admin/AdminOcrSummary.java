package com.foodscanner.application.result.admin;

import java.util.List;

/**
 * Слой: application (результат).
 * Сводка OCR-задач по статусам (для шапки /admin/ocr). Считаются активные задачи; zero-fill.
 * queueSize — активные QUEUED; oldestQueuedAgeSeconds — возраст старейшей QUEUED (backpressure).
 */
public record AdminOcrSummary(
        long total,
        long queueSize,
        long oldestQueuedAgeSeconds,
        List<StatusCount> byStatus
) {
    public record StatusCount(int code, String status, long count) {}
}
