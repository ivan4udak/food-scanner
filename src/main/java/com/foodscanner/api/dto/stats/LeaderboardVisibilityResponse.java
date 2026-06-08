package com.foodscanner.api.dto.stats;

/**
 * Слой: api (DTO).
 * Текущее состояние видимости участника в рейтинге.
 */
public record LeaderboardVisibilityResponse(boolean hiddenFromLeaderboard) {}
