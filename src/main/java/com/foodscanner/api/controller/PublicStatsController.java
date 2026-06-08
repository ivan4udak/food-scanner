package com.foodscanner.api.controller;

import com.foodscanner.application.result.LeaderboardResult;
import com.foodscanner.application.result.PublicStatsResult;
import com.foodscanner.application.usecase.GetLeaderboardUseCase;
import com.foodscanner.application.usecase.GetPublicStatsUseCase;
import com.foodscanner.application.usecase.LeaderboardPeriod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Слой: api.
 *
 * Публичная статистика (без авторизации):
 *   GET /api/v1/public/stats
 *   GET /api/v1/public/leaderboard?period=all|today|week|month&limit=10|50|100
 *
 * Не раскрывает приватных данных (только агрегаты + username).
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicStatsController {

    private final GetPublicStatsUseCase getStats;
    private final GetLeaderboardUseCase getLeaderboard;

    public PublicStatsController(GetPublicStatsUseCase getStats, GetLeaderboardUseCase getLeaderboard) {
        this.getStats = getStats;
        this.getLeaderboard = getLeaderboard;
    }

    @GetMapping("/stats")
    public PublicStatsResult stats() {
        return getStats.execute();
    }

    @GetMapping("/leaderboard")
    public LeaderboardResult leaderboard(
            @RequestParam(name = "period", defaultValue = "all") String period,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return getLeaderboard.execute(LeaderboardPeriod.parse(period), limit);
    }
}
