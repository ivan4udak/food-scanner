package com.foodscanner.application.result;

/**
 * Слой: application (результат).
 * Публичная статистика проекта: всего и за сегодня.
 */
public record PublicStatsResult(Totals totals, Today today) {

    public record Totals(long scans, long catalogEntries, long photos, long contributors) {}

    public record Today(long scans, long catalogEntries, long photos) {}
}
