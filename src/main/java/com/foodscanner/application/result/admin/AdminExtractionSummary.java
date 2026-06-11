package com.foodscanner.application.result.admin;

import java.util.List;

/**
 * Слой: application (результат).
 * Сводка задач извлечения по статусам (для шапки /admin/extraction). Zero-fill.
 */
public record AdminExtractionSummary(long total, List<StatusCount> byStatus) {
    public record StatusCount(int code, String status, long count) {}
}
