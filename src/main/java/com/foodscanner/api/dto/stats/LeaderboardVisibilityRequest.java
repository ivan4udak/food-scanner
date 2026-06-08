package com.foodscanner.api.dto.stats;

import jakarta.validation.constraints.NotNull;

/**
 * Слой: api (DTO).
 * POST /api/v1/me/leaderboard-visibility
 */
public record LeaderboardVisibilityRequest(@NotNull Boolean hidden) {}
