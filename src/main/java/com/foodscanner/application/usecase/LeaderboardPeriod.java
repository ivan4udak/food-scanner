package com.foodscanner.application.usecase;

/**
 * Слой: application.
 * Период публичного рейтинга.
 */
public enum LeaderboardPeriod {
    ALL, TODAY, WEEK, MONTH;

    /** Толерантный парсинг query-параметра (по умолчанию ALL). */
    public static LeaderboardPeriod parse(String raw) {
        if (raw == null) return ALL;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return ALL;
        }
    }

    public String wireName() {
        return name().toLowerCase();
    }
}
