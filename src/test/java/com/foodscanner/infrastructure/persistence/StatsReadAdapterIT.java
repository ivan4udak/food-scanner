package com.foodscanner.infrastructure.persistence;

import com.foodscanner.application.port.StatsReadPort;
import com.foodscanner.application.result.LeaderboardRow;
import com.foodscanner.application.result.PublicStatsResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слой: infrastructure (IT).
 * Проверяет агрегаты публичной статистики и исключение opt-out против реального Postgres.
 */
class StatsReadAdapterIT extends AbstractRepositoryIT {

    @Autowired StatsReadPort port;
    @Autowired JdbcTemplate jdbc;

    @Test
    void leaderboardExcludesHiddenAndPublicStatsCount() {
        UUID alice = contributor("alice_" + UUID.randomUUID(), false);
        UUID bob   = contributor("bob_" + UUID.randomUUID(), true);  // скрыт

        // alice: 2 черновика → 2 записи (3 фото суммарно)
        UUID dA1 = draft(alice); UUID eA1 = entry(alice, dA1); photo(eA1); photo(eA1);
        UUID dA2 = draft(alice); UUID eA2 = entry(alice, dA2); photo(eA2);
        // bob (скрыт): 1 черновик → 1 запись (1 фото)
        UUID dB1 = draft(bob); UUID eB1 = entry(bob, dB1); photo(eB1);

        // alice: брошенный черновик БЕЗ фото — остаётся в БД, но НЕ считается сканом.
        draft(alice);

        List<LeaderboardRow> board = port.leaderboard(null, 10);
        List<String> usernames = board.stream().map(LeaderboardRow::username).toList();
        assertThat(usernames).contains(usernameOf(alice));
        assertThat(usernames).doesNotContain(usernameOf(bob)); // скрытый исключён

        LeaderboardRow aliceRow = board.stream()
            .filter(r -> usernameOf(alice).equals(r.username())).findFirst().orElseThrow();
        assertThat(aliceRow.completedEntries()).isEqualTo(2);
        assertThat(aliceRow.scans()).isEqualTo(2);
        assertThat(aliceRow.uploadedPhotos()).isEqualTo(3);

        PublicStatsResult stats = port.publicStats(Instant.now().minusSeconds(3600));
        // totals считают всех (включая скрытого bob)
        assertThat(stats.totals().catalogEntries()).isGreaterThanOrEqualTo(3);
        assertThat(stats.totals().photos()).isGreaterThanOrEqualTo(4);
        assertThat(stats.totals().scans()).isGreaterThanOrEqualTo(3);
        assertThat(stats.totals().contributors()).isGreaterThanOrEqualTo(2);
    }

    // ── helpers (прямые вставки) ─────────────────────────────
    private String usernameOf(UUID id) {
        return jdbc.queryForObject(
            "SELECT username FROM food_catalog.contributors WHERE id = ?", String.class, id);
    }

    private UUID contributor(String username, boolean hidden) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.contributors
              (id, nickname, username, password_hash, failed_login_attempts,
               completed_catalog_count, hidden_from_leaderboard, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?)""",
            id, username, username, "hash", 0, 0, hidden, now, now);
        return id;
    }

    private UUID draft(UUID contributor) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_drafts
              (id, barcode, contributor_id, status, created_at, updated_at)
            VALUES (?,?,?,?,?,?)""",
            id, "b" + System.nanoTime(), contributor, "COMPLETED", now, now);
        return id;
    }

    private UUID entry(UUID contributor, UUID draftId) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entries
              (id, barcode, contributor_id, draft_id, created_at, updated_at)
            VALUES (?,?,?,?,?,?)""",
            id, "e" + System.nanoTime(), contributor, draftId, now, now);
        return id;
    }

    private void photo(UUID entryId) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entry_photos
              (id, entry_id, type, storage_key, created_at, updated_at)
            VALUES (?,?,?,?,?,?)""",
            UUID.randomUUID(), entryId, "FRONT", "photos/" + UUID.randomUUID(), now, now);
    }
}
