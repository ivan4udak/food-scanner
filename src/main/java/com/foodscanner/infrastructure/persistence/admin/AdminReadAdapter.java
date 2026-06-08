package com.foodscanner.infrastructure.persistence.admin;

import com.foodscanner.application.port.AdminReadPort;
import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
               s.last_seen_at AS last_activity_at, s.client_version, s.browser, s.os, s.device_type,
               (SELECT count(*) FROM food_catalog.catalog_drafts d WHERE d.contributor_id = c.id) AS total_scans,
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
            count("SELECT count(DISTINCT contributor_id) FROM food_catalog.client_sessions WHERE last_seen_at >= ?", online),
            count("SELECT count(DISTINCT contributor_id) FROM food_catalog.client_sessions WHERE last_seen_at >= ?", today),
            count("SELECT count(DISTINCT contributor_id) FROM food_catalog.client_sessions WHERE last_seen_at >= ?", week),
            count("SELECT count(*) FROM food_catalog.catalog_drafts WHERE created_at >= ?", today),
            count("SELECT count(*) FROM food_catalog.catalog_drafts WHERE created_at >= ?", week),
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
            WHERE d.contributor_id = ?
            ORDER BY d.created_at DESC LIMIT ?""",
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

    // ── helpers ──────────────────────────────────────────────
    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
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
