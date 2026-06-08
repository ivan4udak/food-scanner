package com.foodscanner.infrastructure.persistence.me;

import com.foodscanner.application.port.MeReadPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: infrastructure.
 * Read-модель «Мои сканы» (нативный SQL). Все запросы ограничены contributorId.
 */
@Repository
public class MeReadAdapter implements MeReadPort {

    private static final String SCAN_SELECT = """
        SELECT d.barcode, d.status, d.id AS draft_id, e.id AS catalog_entry_id,
               d.created_at AS first_scanned_at, e.created_at AS completed_at,
               COALESCE((SELECT count(*) FROM food_catalog.catalog_entry_photos p WHERE p.entry_id = e.id),
                        (SELECT count(*) FROM food_catalog.draft_photos dp WHERE dp.draft_id = d.id)) AS photo_count
        FROM food_catalog.catalog_drafts d
        LEFT JOIN food_catalog.catalog_entries e ON e.draft_id = d.id
        WHERE d.contributor_id = ?
        """;

    private static final RowMapper<ScanData> SCAN_MAPPER = (rs, n) -> new ScanData(
        rs.getString("barcode"), rs.getString("status"), uuid(rs, "draft_id"), uuid(rs, "catalog_entry_id"),
        inst(rs, "first_scanned_at"), inst(rs, "completed_at"), rs.getLong("photo_count"));

    private static final RowMapper<PhotoData> PHOTO_MAPPER = (rs, n) -> new PhotoData(
        uuid(rs, "id"), rs.getString("type"), rs.getString("storage_key"), inst(rs, "captured_at"));

    private final JdbcTemplate jdbc;

    public MeReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ScanData> scans(UUID contributorId) {
        return jdbc.query(SCAN_SELECT + " ORDER BY d.created_at DESC", SCAN_MAPPER, contributorId);
    }

    @Override
    public Optional<ScanData> scan(UUID contributorId, String barcode) {
        return jdbc.query(SCAN_SELECT + " AND d.barcode = ? ORDER BY d.created_at DESC LIMIT 1",
            SCAN_MAPPER, contributorId, barcode).stream().findFirst();
    }

    @Override
    public List<PhotoData> photos(UUID contributorId, String barcode) {
        List<PhotoData> entryPhotos = jdbc.query("""
            SELECT p.id, p.type, p.storage_key, p.captured_at
            FROM food_catalog.catalog_entry_photos p
            JOIN food_catalog.catalog_entries e ON e.id = p.entry_id
            WHERE e.contributor_id = ? AND e.barcode = ?
            ORDER BY p.type""", PHOTO_MAPPER, contributorId, barcode);
        if (!entryPhotos.isEmpty()) {
            return entryPhotos;
        }
        return jdbc.query("""
            SELECT dp.id, dp.type, dp.storage_key, dp.captured_at
            FROM food_catalog.draft_photos dp
            JOIN food_catalog.catalog_drafts d ON d.id = dp.draft_id
            WHERE d.contributor_id = ? AND d.barcode = ?
            ORDER BY dp.type, dp.created_at DESC""", PHOTO_MAPPER, contributorId, barcode);
    }

    // ── helpers ──────────────────────────────────────────────
    private static UUID uuid(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? null : UUID.fromString(s);
    }
    private static Instant inst(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toInstant();
    }
}
