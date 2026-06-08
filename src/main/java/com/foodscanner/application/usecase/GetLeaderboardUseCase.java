package com.foodscanner.application.usecase;

import com.foodscanner.application.result.LeaderboardResult;

/**
 * Слой: application (use case).
 * Публичный рейтинг участников за период.
 */
public interface GetLeaderboardUseCase {
    LeaderboardResult execute(LeaderboardPeriod period, int limit);
}
