package com.foodscanner.application.port;

import com.foodscanner.application.result.LeaderboardRow;
import com.foodscanner.application.result.PublicStatsResult;

import java.time.Instant;
import java.util.List;

/**
 * Слой: application (порт чтения).
 * Агрегаты публичной статистики. Реализация — нативные запросы в infrastructure.
 */
public interface StatsReadPort {

    PublicStatsResult publicStats(Instant todayStart);

    /**
     * Топ участников.
     * @param since   нижняя граница периода (null — за всё время)
     * @param limit   максимум строк
     */
    List<LeaderboardRow> leaderboard(Instant since, int limit);
}
