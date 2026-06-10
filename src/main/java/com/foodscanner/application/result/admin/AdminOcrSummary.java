package com.foodscanner.application.result.admin;

import java.util.List;

/**
 * Слой: application (результат).
 * Сводка OCR-задач по статусам (для шапки страницы /admin/ocr). Zero-fill включён.
 */
public record AdminOcrSummary(long total, List<StatusCount> byStatus) {
    public record StatusCount(int code, String status, long count) {}
}
