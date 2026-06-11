package com.foodscanner.infrastructure.persistence.admin;

import com.foodscanner.application.port.AdminReadPort;
import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.ocr.OcrStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: infrastructure.
 * Read-модель админ-панели на нативном SQL (операционная наблюдаемость).
 * Только SELECT; данные не изменяются.
 */
@Repository
public class AdminReadAdapter implements AdminReadPort {

    /** Поля строки пользователя (используется и в списке, и в карточке). */
    private static final String USER_SELECT = """
        SELECT c.id, c.username, c.role,
               GREATEST(
                   s.last_seen_at,
                   (SELECT max(ca.occurred_at) FROM food_catalog.client_activity ca
                      WHERE ca.contributor_id = c.id)
               ) AS last_activity_at,
               s.client_version, s.browser, s.os, s.device_type,
               (SELECT count(*) FROM food_catalog.catalog_drafts d
                  WHERE d.contributor_id = c.id
                    AND (EXISTS (SELECT 1 FROM food_catalog.draft_photos dp WHERE dp.draft_id = d.id)
                         OR EXISTS (SELECT 1 FROM food_catalog.catalog_entries ce WHERE ce.draft_id = d.id))
               ) AS total_scans,
               (SELECT count(*) FROM food_catalog.catalog_entries e WHERE e.contributor_id = c.id) AS completed_entries,
               (SELECT count(*) FROM food_catalog.catalog_entry_photos p
                  JOIN food_catalog.catalog_entries e2 ON e2.id = p.entry_id
                  WHERE e2.contributor_id = c.id) AS uploaded_photos,
               (SELECT count(*) FROM food_catalog.client_logs l
                  WHERE l.contributor_id = c.id AND l.level IN ('WARN','ERROR')) AS client_errors
        FROM food_catalog.contributors c
        LEFT JOIN LATERAL (
            SELECT cs.* FROM food_catalog.client_sessions cs
            WHERE cs.contributor_id = c.id ORDER BY cs.last_seen_at DESC LIMIT 1
        ) s ON true
        """;

    private static final String CLIENT_LOG_SELECT = """
        SELECT l.id, l.contributor_id, c.username, l.session_id, l.correlation_id, l."timestamp",
               l.level, l.category, l.event, l.screen, l.message, l.metadata_json, l.duration_ms,
               l.stack_trace, l.barcode, l.api_method, l.api_path, l.http_status
        FROM food_catalog.client_logs l
        LEFT JOIN food_catalog.contributors c ON c.id = l.contributor_id
        """;

    private static final String SERVER_EVENT_SELECT = """
        SELECT id, occurred_at, level, event, correlation_id, contributor_id, username, method, path,
               http_status, duration_ms, use_case, barcode, error_code, error_message, exception_class
        FROM food_catalog.server_events
        """;

    /** Скан засчитывается только при наличии фото в черновике (или завершении). Алиас — d. */
    private static final String DRAFT_HAS_PHOTO =
        "(EXISTS (SELECT 1 FROM food_catalog.draft_photos dp WHERE dp.draft_id = d.id)"
        + " OR EXISTS (SELECT 1 FROM food_catalog.catalog_entries ce WHERE ce.draft_id = d.id))";

    private final JdbcTemplate jdbc;

    public AdminReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Dashboard ────────────────────────────────────────────
    @Override
    public AdminDashboard dashboard(Instant todayStart, Instant weekStart, Instant onlineSince) {
        Timestamp today = Timestamp.from(todayStart);
        Timestamp week = Timestamp.from(weekStart);
        Timestamp online = Timestamp.from(onlineSince);
        return new AdminDashboard(
            count("SELECT count(*) FROM food_catalog.contributors"),
            presenceCount(online),
            presenceCount(today),
            presenceCount(week),
            count("SELECT count(*) FROM food_catalog.catalog_drafts d WHERE d.created_at >= ? AND " + DRAFT_HAS_PHOTO, today),
            count("SELECT count(*) FROM food_catalog.catalog_drafts d WHERE d.created_at >= ? AND " + DRAFT_HAS_PHOTO, week),
            count("SELECT count(*) FROM food_catalog.catalog_entries WHERE created_at >= ?", today),
            count("SELECT count(*) FROM food_catalog.catalog_entries WHERE created_at >= ?", week),
            count("SELECT count(*) FROM food_catalog.catalog_entry_photos WHERE created_at >= ?", today),
            count("SELECT count(*) FROM food_catalog.client_logs WHERE level IN ('WARN','ERROR') AND \"timestamp\" >= ?", today),
            count("SELECT count(*) FROM food_catalog.server_events WHERE level IN ('WARN','ERROR') AND occurred_at >= ?", today));
    }

    // ── Users ────────────────────────────────────────────────
    @Override
    public List<AdminUserRow> users(Instant onlineSince, String sort, int limit, int offset) {
        String sql = USER_SELECT + " WHERE c.username IS NOT NULL ORDER BY " + orderBy(sort) + " LIMIT ? OFFSET ?";
        return jdbc.query(sql, userMapper(onlineSince), limit, offset);
    }

    @Override
    public Optional<AdminUserRow> user(UUID id, Instant onlineSince) {
        String sql = USER_SELECT + " WHERE c.id = ?";
        List<AdminUserRow> rows = jdbc.query(sql, userMapper(onlineSince), id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<AdminUserRow> userByUsername(String username, Instant onlineSince) {
        String sql = USER_SELECT + " WHERE c.username = ?";
        return jdbc.query(sql, userMapper(onlineSince), username).stream().findFirst();
    }

    private RowMapper<AdminUserRow> userMapper(Instant onlineSince) {
        return (rs, n) -> {
            Instant last = inst(rs, "last_activity_at");
            boolean online = last != null && !last.isBefore(onlineSince);
            return new AdminUserRow(
                uuid(rs, "id"), str(rs, "username"), str(rs, "role"), online, last,
                str(rs, "client_version"), str(rs, "browser"), str(rs, "os"), str(rs, "device_type"),
                lng(rs, "total_scans"), lng(rs, "completed_entries"),
                lng(rs, "uploaded_photos"), lng(rs, "client_errors"));
        };
    }

    private static String orderBy(String sort) {
        return switch (sort == null ? "" : sort) {
            case "completedEntries" -> "completed_entries DESC";
            case "uploadedPhotos" -> "uploaded_photos DESC";
            case "totalScans" -> "total_scans DESC";
            case "clientErrors" -> "client_errors DESC";
            default -> "last_activity_at DESC NULLS LAST";
        };
    }

    @Override
    public List<AdminSessionRow> sessions(UUID contributorId, int limit) {
        return jdbc.query("""
            SELECT session_id, started_at, last_seen_at, client_version, browser, os, device_type,
                   network_status, standalone
            FROM food_catalog.client_sessions WHERE contributor_id = ?
            ORDER BY last_seen_at DESC LIMIT ?""",
            (rs, n) -> new AdminSessionRow(
                uuid(rs, "session_id"), inst(rs, "started_at"), inst(rs, "last_seen_at"),
                str(rs, "client_version"), str(rs, "browser"), str(rs, "os"), str(rs, "device_type"),
                str(rs, "network_status"), bool(rs, "standalone")),
            contributorId, limit);
    }

    @Override
    public List<AdminScanRow> scans(UUID contributorId, int limit) {
        return jdbc.query("""
            SELECT d.barcode, d.status, d.id AS draft_id, e.id AS catalog_entry_id,
                   d.created_at AS first_scanned_at, e.created_at AS completed_at,
                   COALESCE((SELECT count(*) FROM food_catalog.catalog_entry_photos p WHERE p.entry_id = e.id),
                            (SELECT count(*) FROM food_catalog.draft_photos dp WHERE dp.draft_id = d.id)) AS photo_count
            FROM food_catalog.catalog_drafts d
            LEFT JOIN food_catalog.catalog_entries e ON e.draft_id = d.id
            WHERE d.contributor_id = ? AND %s
            ORDER BY d.created_at DESC LIMIT ?""".formatted(DRAFT_HAS_PHOTO),
            (rs, n) -> {
                UUID entryId = uuid(rs, "catalog_entry_id");
                String rawStatus = str(rs, "status");
                String status = entryId != null ? "COMPLETED"
                    : "OPEN".equalsIgnoreCase(rawStatus) ? "DRAFT_OPEN" : rawStatus;
                return new AdminScanRow(str(rs, "barcode"), status, uuid(rs, "draft_id"), entryId,
                    inst(rs, "first_scanned_at"), inst(rs, "completed_at"), lng(rs, "photo_count"));
            },
            contributorId, limit);
    }

    // ── Client logs ──────────────────────────────────────────
    @Override
    public List<AdminClientLog> clientLogs(AdminLogFilter f) {
        StringBuilder sql = new StringBuilder(CLIENT_LOG_SELECT).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        addEq(sql, args, "l.contributor_id", f.contributorId());
        addEq(sql, args, "l.session_id", f.sessionId());
        addEq(sql, args, "l.level", f.level());
        addEq(sql, args, "l.category", f.category());
        addEq(sql, args, "l.event", f.event());
        addEq(sql, args, "l.barcode", f.barcode());
        addEq(sql, args, "l.screen", f.screen());
        if (f.dateFrom() != null) { sql.append(" AND l.\"timestamp\" >= ?"); args.add(Timestamp.from(f.dateFrom())); }
        if (f.dateTo() != null)   { sql.append(" AND l.\"timestamp\" <= ?"); args.add(Timestamp.from(f.dateTo())); }
        sql.append(" ORDER BY l.\"timestamp\" DESC LIMIT ? OFFSET ?");
        args.add(f.limit());
        args.add(f.offset());
        return jdbc.query(sql.toString(), CLIENT_LOG_MAPPER, args.toArray());
    }

    @Override
    public List<AdminClientLog> clientLogsByCorrelation(UUID correlationId) {
        return jdbc.query(CLIENT_LOG_SELECT + " WHERE l.correlation_id = ? ORDER BY l.\"timestamp\"",
            CLIENT_LOG_MAPPER, correlationId);
    }

    @Override
    public List<AdminClientLog> clientLogsByBarcode(String barcode, int limit) {
        return jdbc.query(CLIENT_LOG_SELECT + " WHERE l.barcode = ? ORDER BY l.\"timestamp\" DESC LIMIT ?",
            CLIENT_LOG_MAPPER, barcode, limit);
    }

    @Override
    public List<AdminClientLog> clientErrors(Instant since, int limit) {
        return jdbc.query(CLIENT_LOG_SELECT
                + " WHERE l.level IN ('WARN','ERROR') AND l.\"timestamp\" >= ? ORDER BY l.\"timestamp\" DESC LIMIT ?",
            CLIENT_LOG_MAPPER, Timestamp.from(since), limit);
    }

    @Override
    public List<AdminClientLog> clientErrorsByUser(UUID contributorId, int limit) {
        return jdbc.query(CLIENT_LOG_SELECT
                + " WHERE l.contributor_id = ? AND l.level IN ('WARN','ERROR') ORDER BY l.\"timestamp\" DESC LIMIT ?",
            CLIENT_LOG_MAPPER, contributorId, limit);
    }

    private static final RowMapper<AdminClientLog> CLIENT_LOG_MAPPER = (rs, n) -> new AdminClientLog(
        uuid(rs, "id"), uuid(rs, "contributor_id"), str(rs, "username"), uuid(rs, "session_id"),
        uuid(rs, "correlation_id"), inst(rs, "timestamp"), str(rs, "level"), str(rs, "category"),
        str(rs, "event"), str(rs, "screen"), str(rs, "message"), str(rs, "metadata_json"),
        lng(rs, "duration_ms"), str(rs, "stack_trace"), str(rs, "barcode"),
        str(rs, "api_method"), str(rs, "api_path"), integer(rs, "http_status"));

    // ── Server events ────────────────────────────────────────
    @Override
    public List<AdminServerEventRow> serverEventsByCorrelation(UUID correlationId) {
        return jdbc.query(SERVER_EVENT_SELECT + " WHERE correlation_id = ? ORDER BY occurred_at",
            SERVER_EVENT_MAPPER, correlationId);
    }

    @Override
    public List<AdminServerEventRow> serverErrors(Instant since, int limit) {
        return jdbc.query(SERVER_EVENT_SELECT
                + " WHERE level IN ('WARN','ERROR') AND occurred_at >= ? ORDER BY occurred_at DESC LIMIT ?",
            SERVER_EVENT_MAPPER, Timestamp.from(since), limit);
    }

    private static final RowMapper<AdminServerEventRow> SERVER_EVENT_MAPPER = (rs, n) -> new AdminServerEventRow(
        uuid(rs, "id"), inst(rs, "occurred_at"), str(rs, "level"), str(rs, "event"),
        uuid(rs, "correlation_id"), uuid(rs, "contributor_id"), str(rs, "username"),
        str(rs, "method"), str(rs, "path"), integer(rs, "http_status"), lng(rs, "duration_ms"),
        str(rs, "use_case"), str(rs, "barcode"), str(rs, "error_code"), str(rs, "error_message"),
        str(rs, "exception_class"));

    // ── Catalog ──────────────────────────────────────────────
    private static final String CATALOG_SELECT = """
        SELECT e.id, e.barcode, e.contributor_id, c.username AS author, e.created_at,
               (SELECT count(*) FROM food_catalog.catalog_entry_photos p WHERE p.entry_id = e.id) AS photo_count
        FROM food_catalog.catalog_entries e
        LEFT JOIN food_catalog.contributors c ON c.id = e.contributor_id
        """;
    private static final RowMapper<AdminCatalogRow> CATALOG_MAPPER = (rs, n) -> new AdminCatalogRow(
        uuid(rs, "id"), str(rs, "barcode"), uuid(rs, "contributor_id"), str(rs, "author"),
        inst(rs, "created_at"), lng(rs, "photo_count"));

    @Override
    public List<AdminCatalogRow> catalog(int limit, int offset) {
        return jdbc.query(CATALOG_SELECT + " ORDER BY e.created_at DESC LIMIT ? OFFSET ?",
            CATALOG_MAPPER, limit, offset);
    }

    @Override
    public Optional<AdminCatalogRow> catalogByBarcode(String barcode) {
        return jdbc.query(CATALOG_SELECT + " WHERE e.barcode = ?", CATALOG_MAPPER, barcode)
            .stream().findFirst();
    }

    @Override
    public List<AdminCatalogDetail.Photo> catalogPhotos(UUID catalogEntryId) {
        return jdbc.query("""
            SELECT id, type, storage_key, created_at
            FROM food_catalog.catalog_entry_photos WHERE entry_id = ? ORDER BY type""",
            (rs, n) -> new AdminCatalogDetail.Photo(
                uuid(rs, "id"), str(rs, "type"), str(rs, "storage_key"), inst(rs, "created_at")),
            catalogEntryId);
    }

    // ── OCR ──────────────────────────────────────────────────
    private static final String OCR_SELECT = """
        SELECT oj.id, COALESCE(d.barcode, e.barcode) AS barcode,
               COALESCE(d.contributor_id, e.contributor_id) AS contributor_id,
               COALESCE(cd.username, ce.username) AS author,
               oj.draft_id, oj.catalog_entry_id, oj.photo_type, oj.storage_key, oj.status, oj.attempts,
               oj.active, oj.orphaned, oj.updated_at, oj.error_code, oj.error_message,
               left(oj.raw_text, 200) AS raw_text_preview
        FROM food_catalog.ocr_jobs oj
        LEFT JOIN food_catalog.catalog_drafts d ON d.id = oj.draft_id
        LEFT JOIN food_catalog.catalog_entries e ON e.id = oj.catalog_entry_id
        LEFT JOIN food_catalog.contributors cd ON cd.id = d.contributor_id
        LEFT JOIN food_catalog.contributors ce ON ce.id = e.contributor_id
        """;
    private static final RowMapper<AdminOcrRow> OCR_MAPPER = (rs, n) -> {
        int code = rs.getInt("status");
        return new AdminOcrRow(
            uuid(rs, "id"), str(rs, "barcode"), uuid(rs, "contributor_id"), str(rs, "author"),
            uuid(rs, "draft_id"), uuid(rs, "catalog_entry_id"),
            str(rs, "photo_type"), str(rs, "storage_key"), code, OcrStatus.fromCode(code).name(),
            rs.getInt("attempts"), rs.getBoolean("active"), rs.getBoolean("orphaned"),
            inst(rs, "updated_at"), str(rs, "error_code"), str(rs, "error_message"),
            str(rs, "raw_text_preview"));
    };

    @Override
    public List<AdminOcrRow> ocrJobs(Integer status, String barcode, boolean includeInactive,
                                     boolean includeOrphaned, int limit, int offset) {
        StringBuilder sql = new StringBuilder(OCR_SELECT);
        List<Object> args = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (!includeInactive) where.add("oj.active = true");
        if (!includeOrphaned) where.add("oj.orphaned = false");
        if (status != null) {
            where.add("oj.status = ?");
            args.add(status);
        }
        if (barcode != null && !barcode.isBlank()) {
            where.add("COALESCE(d.barcode, e.barcode) = ?");
            args.add(barcode.trim());
        }
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY oj.updated_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), OCR_MAPPER, args.toArray());
    }

    @Override
    public AdminOcrSummary ocrSummary() {
        // считаем активные не-orphan задачи (актуальная очередь/состояние)
        Map<Integer, Long> counts = new HashMap<>();
        jdbc.query("""
            SELECT status, count(*) AS cnt FROM food_catalog.ocr_jobs
            WHERE active = true AND orphaned = false GROUP BY status""",
            (rs, n) -> counts.put(rs.getInt("status"), rs.getLong("cnt")));
        long total = 0;
        List<AdminOcrSummary.StatusCount> byStatus = new ArrayList<>();
        for (OcrStatus s : OcrStatus.values()) {
            long c = counts.getOrDefault(s.code(), 0L);
            total += c;
            byStatus.add(new AdminOcrSummary.StatusCount(s.code(), s.name(), c));
        }
        long queueSize = counts.getOrDefault(OcrStatus.QUEUED.code(), 0L);
        Long oldestEpoch = jdbc.queryForObject("""
            SELECT EXTRACT(EPOCH FROM (now() - min(created_at)))::bigint
            FROM food_catalog.ocr_jobs WHERE active = true AND orphaned = false AND status = 0""",
            Long.class);
        long oldestAge = oldestEpoch == null ? 0 : Math.max(0, oldestEpoch);
        return new AdminOcrSummary(total, queueSize, oldestAge, byStatus);
    }

    @Override
    public List<AdminOcrRow> ocrJobsByBarcode(String barcode, int limit) {
        return jdbc.query(OCR_SELECT + " WHERE COALESCE(d.barcode, e.barcode) = ?"
            + " ORDER BY oj.updated_at DESC LIMIT ?", OCR_MAPPER, barcode, limit);
    }

    @Override
    public List<AdminOcrRow> ocrJobsByUser(UUID contributorId, int limit) {
        return jdbc.query(OCR_SELECT + " WHERE COALESCE(d.contributor_id, e.contributor_id) = ?"
            + " ORDER BY oj.updated_at DESC LIMIT ?", OCR_MAPPER, contributorId, limit);
    }

    @Override
    public Optional<AdminOcrDetail> ocrById(UUID jobId) {
        String sql = """
            SELECT oj.id, COALESCE(d.barcode, e.barcode) AS barcode,
                   COALESCE(d.contributor_id, e.contributor_id) AS contributor_id,
                   COALESCE(cd.username, ce.username) AS author,
                   oj.draft_id, oj.catalog_entry_id, oj.photo_type, oj.storage_key, oj.status, oj.attempts,
                   oj.active, oj.orphaned, oj.confidence, oj.created_at, oj.updated_at,
                   oj.error_code, oj.error_message, oj.raw_text,
                   oj.parsed_name, oj.parsed_brand, oj.parsed_manufacturer,
                   oj.parsed_ingredients, oj.parsed_nutrition,
                   oj.published_at, oj.publish_attempts, oj.last_publish_error, oj.superseded_at, oj.superseded_by
            FROM food_catalog.ocr_jobs oj
            LEFT JOIN food_catalog.catalog_drafts d ON d.id = oj.draft_id
            LEFT JOIN food_catalog.catalog_entries e ON e.id = oj.catalog_entry_id
            LEFT JOIN food_catalog.contributors cd ON cd.id = d.contributor_id
            LEFT JOIN food_catalog.contributors ce ON ce.id = e.contributor_id
            WHERE oj.id = ?""";
        return jdbc.query(sql, (rs, n) -> {
            int code = rs.getInt("status");
            return new AdminOcrDetail(
                uuid(rs, "id"), str(rs, "barcode"), uuid(rs, "contributor_id"), str(rs, "author"),
                uuid(rs, "draft_id"), uuid(rs, "catalog_entry_id"), str(rs, "photo_type"),
                str(rs, "storage_key"), code, OcrStatus.fromCode(code).name(), rs.getInt("attempts"),
                rs.getBoolean("active"), rs.getBoolean("orphaned"), (Double) rs.getObject("confidence"),
                inst(rs, "created_at"), inst(rs, "updated_at"), str(rs, "error_code"), str(rs, "error_message"),
                str(rs, "raw_text"), str(rs, "parsed_name"), str(rs, "parsed_brand"), str(rs, "parsed_manufacturer"),
                str(rs, "parsed_ingredients"), str(rs, "parsed_nutrition"),
                inst(rs, "published_at"), rs.getInt("publish_attempts"), str(rs, "last_publish_error"),
                inst(rs, "superseded_at"), uuid(rs, "superseded_by"));
        }, jobId).stream().findFirst();
    }

    // ── product extraction ───────────────────────────────────
    private static final String EXTRACTION_SELECT = """
        SELECT pej.id, pej.ocr_job_id, pej.barcode, pej.type, pej.status, pej.attempts,
               pej.source, pej.name, pej.brand, pej.manufacturer, pej.last_error,
               pej.queued_at, pej.processed_at, pej.updated_at
        FROM food_catalog.product_extraction_jobs pej
        """;
    private static final RowMapper<AdminExtractionRow> EXTRACTION_MAPPER = (rs, n) -> {
        int code = rs.getInt("status");
        return new AdminExtractionRow(
            uuid(rs, "id"), uuid(rs, "ocr_job_id"), str(rs, "barcode"), str(rs, "type"),
            code, ExtractionStatus.fromCode(code).name(), rs.getInt("attempts"),
            str(rs, "source"), str(rs, "name"), str(rs, "brand"), str(rs, "manufacturer"),
            str(rs, "last_error"), inst(rs, "queued_at"), inst(rs, "processed_at"),
            inst(rs, "updated_at"));
    };

    @Override
    public List<AdminExtractionRow> extractionJobs(Integer status, String type, String barcode,
                                                   int limit, int offset) {
        StringBuilder sql = new StringBuilder(EXTRACTION_SELECT);
        List<Object> args = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (status != null) {
            where.add("pej.status = ?");
            args.add(status);
        }
        if (type != null && !type.isBlank()) {
            where.add("pej.type = ?");
            args.add(type.trim());
        }
        if (barcode != null && !barcode.isBlank()) {
            where.add("pej.barcode = ?");
            args.add(barcode.trim());
        }
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY pej.queued_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), EXTRACTION_MAPPER, args.toArray());
    }

    @Override
    public AdminExtractionSummary extractionSummary() {
        Map<Integer, Long> counts = new HashMap<>();
        jdbc.query("SELECT status, count(*) AS cnt FROM food_catalog.product_extraction_jobs GROUP BY status",
            (rs, n) -> counts.put(rs.getInt("status"), rs.getLong("cnt")));
        long total = 0;
        List<AdminExtractionSummary.StatusCount> byStatus = new ArrayList<>();
        for (ExtractionStatus s : ExtractionStatus.values()) {
            long c = counts.getOrDefault(s.code(), 0L);
            total += c;
            byStatus.add(new AdminExtractionSummary.StatusCount(s.code(), s.name(), c));
        }
        return new AdminExtractionSummary(total, byStatus);
    }

    // ── helpers ──────────────────────────────────────────────
    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    /** Присутствие = сессии ИЛИ heartbeat-активность за окно (устойчиво к незавершённой сессии). */
    private long presenceCount(Timestamp since) {
        return count("""
            SELECT count(DISTINCT contributor_id) FROM (
                SELECT contributor_id FROM food_catalog.client_sessions WHERE last_seen_at >= ?
                UNION ALL
                SELECT contributor_id FROM food_catalog.client_activity WHERE occurred_at >= ?
            ) x""", since, since);
    }

    private static void addEq(StringBuilder sql, List<Object> args, String col, Object value) {
        if (value != null) {
            sql.append(" AND ").append(col).append(" = ?");
            args.add(value);
        }
    }

    private static String str(ResultSet rs, String col) throws SQLException { return rs.getString(col); }
    private static UUID uuid(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? null : UUID.fromString(s);
    }
    private static Instant inst(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toInstant();
    }
    private static Integer integer(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : ((Number) o).intValue();
    }
    private static Long lng(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : ((Number) o).longValue();
    }
    private static Boolean bool(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : (Boolean) o;
    }
}
