package com.foodscanner.infrastructure.persistence.stats;

import com.foodscanner.application.port.StatsReadPort;
import com.foodscanner.application.result.LeaderboardRow;
import com.foodscanner.application.result.PublicStatsResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Слой: infrastructure.
 *
 * Read-модель публичной статистики (нативные агрегаты). Метрики:
 *   scans         ≈ catalog_drafts (скан, создавший черновик),
 *   catalogEntries = catalog_entries,
 *   photos         = catalog_entry_photos.
 * В рейтинге участвуют только пользователи с username и без opt-out.
 */
@Repository
public class StatsReadAdapter implements StatsReadPort {

    private final JdbcTemplate jdbc;

    /**
     * Скан засчитывается только если в черновике есть хотя бы одно фото
     * (или черновик завершён в запись каталога). Пустые брошенные черновики
     * остаются в БД, но не учитываются как скан в статистике. Алиас черновика — d.
     */
    private static final String DRAFT_HAS_PHOTO =
        "(EXISTS (SELECT 1 FROM food_catalog.draft_photos dp WHERE dp.draft_id = d.id)"
        + " OR EXISTS (SELECT 1 FROM food_catalog.catalog_entries ce WHERE ce.draft_id = d.id))";

    public StatsReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PublicStatsResult publicStats(Instant todayStart) {
        Timestamp start = Timestamp.from(todayStart);
        long scans       = count("SELECT count(*) FROM food_catalog.catalog_drafts d WHERE " + DRAFT_HAS_PHOTO);
        long entries     = count("SELECT count(*) FROM food_catalog.catalog_entries");
        long photos      = count("SELECT count(*) FROM food_catalog.catalog_entry_photos");
        long contributors = count("SELECT count(*) FROM food_catalog.contributors");
        long scansToday   = count("SELECT count(*) FROM food_catalog.catalog_drafts d WHERE d.created_at >= ? AND " + DRAFT_HAS_PHOTO, start);
        long entriesToday = count("SELECT count(*) FROM food_catalog.catalog_entries WHERE created_at >= ?", start);
        long photosToday  = count("SELECT count(*) FROM food_catalog.catalog_entry_photos WHERE created_at >= ?", start);

        return new PublicStatsResult(
            new PublicStatsResult.Totals(scans, entries, photos, contributors),
            new PublicStatsResult.Today(scansToday, entriesToday, photosToday));
    }

    @Override
    public List<LeaderboardRow> leaderboard(Instant since, int limit) {
        boolean filtered = since != null;
        String entryFilter = filtered ? " AND e.created_at >= ?" : "";
        // Скан в рейтинге — только черновики с фото (или завершённые); пустые не считаем.
        String draftFilter = (filtered ? " AND d.created_at >= ?" : "") + " AND " + DRAFT_HAS_PHOTO;
        String photoFilter = filtered ? " AND p.created_at >= ?" : "";

        String sql = """
            SELECT c.username AS username,
              (SELECT count(*) FROM food_catalog.catalog_entries e
                 WHERE e.contributor_id = c.id%s) AS completed,
              (SELECT count(*) FROM food_catalog.catalog_drafts d
                 WHERE d.contributor_id = c.id%s) AS scans,
              (SELECT count(*) FROM food_catalog.catalog_entry_photos p
                 JOIN food_catalog.catalog_entries e2 ON e2.id = p.entry_id
                 WHERE e2.contributor_id = c.id%s) AS photos
            FROM food_catalog.contributors c
            WHERE c.username IS NOT NULL AND c.hidden_from_leaderboard = FALSE
            ORDER BY completed DESC, photos DESC, scans DESC
            LIMIT ?
            """.formatted(entryFilter, draftFilter, photoFilter);

        Object[] args;
        if (filtered) {
            Timestamp ts = Timestamp.from(since);
            args = new Object[]{ts, ts, ts, limit};
        } else {
            args = new Object[]{limit};
        }

        List<LeaderboardRow> rows = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rows.add(new LeaderboardRow(
                rs.getString("username"),
                rs.getLong("completed"),
                rs.getLong("scans"),
                rs.getLong("photos")));
        }, args);
        return rows;
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }
}
