package com.foodscanner.application.service;

import com.foodscanner.application.port.StatsReadPort;
import com.foodscanner.application.result.LeaderboardResult;
import com.foodscanner.application.result.LeaderboardRow;
import com.foodscanner.application.result.PublicStatsResult;
import com.foodscanner.application.usecase.GetLeaderboardUseCase;
import com.foodscanner.application.usecase.GetPublicStatsUseCase;
import com.foodscanner.application.usecase.LeaderboardPeriod;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Слой: application.
 *
 * Публичная статистика и рейтинг. Score = completedEntries (primary),
 * tiebreak — uploadedPhotos, затем scans. Скрытые пользователи отсеяны в порту.
 */
@Service
public class PublicStatsService implements GetPublicStatsUseCase, GetLeaderboardUseCase {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 10;

    private final StatsReadPort port;

    public PublicStatsService(StatsReadPort port) {
        this.port = port;
    }

    @Override
    public PublicStatsResult execute() {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return port.publicStats(todayStart);
    }

    @Override
    public LeaderboardResult execute(LeaderboardPeriod period, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<LeaderboardRow> rows = port.leaderboard(sinceFor(period), safeLimit);

        List<LeaderboardResult.Entry> items = new ArrayList<>(rows.size());
        int rank = 1;
        for (LeaderboardRow r : rows) {
            items.add(new LeaderboardResult.Entry(
                rank++, r.username(), r.completedEntries(), r.scans(), r.uploadedPhotos(),
                r.completedEntries())); // score = completedEntries
        }
        return new LeaderboardResult(period.wireName(), items);
    }

    private static Instant sinceFor(LeaderboardPeriod period) {
        Instant now = Instant.now();
        return switch (period) {
            case ALL -> null;
            case TODAY -> LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            case WEEK -> now.minus(Duration.ofDays(7));
            case MONTH -> now.minus(Duration.ofDays(30));
        };
    }
}
