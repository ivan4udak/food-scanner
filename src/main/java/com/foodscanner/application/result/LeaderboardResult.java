package com.foodscanner.application.result;

import java.util.List;

/**
 * Слой: application (результат).
 * Рейтинг участников за период.
 */
public record LeaderboardResult(String period, List<Entry> items) {

    public record Entry(int rank, String username, long completedEntries,
                        long scans, long uploadedPhotos, long score) {}
}
