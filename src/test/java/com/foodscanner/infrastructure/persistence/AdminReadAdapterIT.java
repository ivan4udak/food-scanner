package com.foodscanner.infrastructure.persistence;

import com.foodscanner.application.port.AdminReadPort;
import com.foodscanner.application.query.AdminLogFilter;
import com.foodscanner.application.result.admin.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слой: infrastructure (IT). Проверяет read-SQL админки против реального Postgres.
 */
class AdminReadAdapterIT extends AbstractRepositoryIT {

    @Autowired AdminReadPort port;
    @Autowired JdbcTemplate jdbc;

    @Test
    void dashboardUsersScansLogsTraceCatalog() {
        Instant now = Instant.now();
        UUID ivan = contributor("ivan_" + UUID.randomUUID());
        session(ivan, now);                       // онлайн (last_seen = сейчас)
        UUID draft = draft(ivan);
        UUID entry = entry(ivan, draft);
        UUID barcodeEntry = entryBarcode(entry);
        photo(entry);
        UUID corr = UUID.randomUUID();
        clientLog(ivan, "INFO", "NETWORK", "API_RESPONSE", corr, barcodeStr(draft));
        clientLog(ivan, "ERROR", "PHOTO", "PHOTO_UPLOAD_FAILED", null, null);
        serverEvent(ivan, "INFO", "SCAN_COMPLETED", corr);

        // dashboard
        AdminDashboard d = port.dashboard(now.minus(1, ChronoUnit.HOURS),
            now.minus(7, ChronoUnit.DAYS), now.minus(5, ChronoUnit.MINUTES));
        assertThat(d.usersTotal()).isGreaterThanOrEqualTo(1);
        assertThat(d.onlineNow()).isGreaterThanOrEqualTo(1);
        assertThat(d.entriesToday()).isGreaterThanOrEqualTo(1);
        assertThat(d.clientErrorsToday()).isGreaterThanOrEqualTo(1);

        // users
        AdminUserRow row = port.user(ivan, now.minus(5, ChronoUnit.MINUTES)).orElseThrow();
        assertThat(row.online()).isTrue();
        assertThat(row.completedEntries()).isEqualTo(1);
        assertThat(row.uploadedPhotos()).isEqualTo(1);
        assertThat(row.totalScans()).isEqualTo(1);
        assertThat(row.clientErrors()).isEqualTo(1);

        // scans
        List<AdminScanRow> scans = port.scans(ivan, 10);
        assertThat(scans).hasSize(1);
        assertThat(scans.get(0).status()).isEqualTo("COMPLETED");
        assertThat(scans.get(0).photoCount()).isEqualTo(1);

        // logs filter
        List<AdminClientLog> errors = port.clientLogs(
            AdminLogFilter.builder().contributorId(ivan).level("ERROR").build());
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).event()).isEqualTo("PHOTO_UPLOAD_FAILED");

        // trace
        assertThat(port.clientLogsByCorrelation(corr)).hasSize(1);
        assertThat(port.serverEventsByCorrelation(corr)).hasSize(1);

        // catalog
        String bc = barcodeStr2(entry);
        AdminCatalogRow cat = port.catalogByBarcode(bc).orElseThrow();
        assertThat(cat.author()).isEqualTo(usernameOf(ivan));
        assertThat(port.catalogPhotos(barcodeEntry)).hasSize(1);
    }

    // ── helpers ──────────────────────────────────────────────
    private String usernameOf(UUID id) {
        return jdbc.queryForObject("SELECT username FROM food_catalog.contributors WHERE id=?", String.class, id);
    }

    private UUID contributor(String username) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.contributors
              (id,nickname,username,password_hash,failed_login_attempts,completed_catalog_count,
               hidden_from_leaderboard,role,created_at,updated_at)
            VALUES (?,?,?,?,0,0,false,'USER',?,?)""", id, username, username, "h", now, now);
        return id;
    }

    private void session(UUID c, Instant lastSeen) {
        Timestamp ts = Timestamp.from(lastSeen);
        jdbc.update("""
            INSERT INTO food_catalog.client_sessions
              (id,contributor_id,session_id,started_at,last_seen_at,client_version,browser,os,device_type,standalone)
            VALUES (?,?,?,?,?,?,?,?,?,?)""",
            UUID.randomUUID(), c, UUID.randomUUID(), ts, ts, "1.7.0", "Safari", "iOS", "mobile", true);
    }

    private UUID draft(UUID c) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_drafts (id,barcode,contributor_id,status,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", id, "bc" + System.nanoTime(), c, "COMPLETED", now, now);
        return id;
    }

    private UUID entry(UUID c, UUID draftId) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        String bc = "ec" + System.nanoTime();
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entries (id,barcode,contributor_id,draft_id,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", id, bc, c, draftId, now, now);
        entryBarcodeCache = bc;
        return id;
    }

    private String entryBarcodeCache;
    private String barcodeStr2(UUID entryId) { return entryBarcodeCache; }
    private UUID entryBarcode(UUID entryId) { return entryId; }
    private String barcodeStr(UUID draftId) {
        return jdbc.queryForObject("SELECT barcode FROM food_catalog.catalog_drafts WHERE id=?", String.class, draftId);
    }

    private void photo(UUID entryId) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entry_photos (id,entry_id,type,storage_key,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", UUID.randomUUID(), entryId, "FRONT", "photos/" + UUID.randomUUID(), now, now);
    }

    private void clientLog(UUID c, String level, String category, String event, UUID corr, String barcode) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.client_logs
              (id,contributor_id,session_id,correlation_id,"timestamp",received_at,level,category,event,barcode)
            VALUES (?,?,?,?,?,?,?,?,?,?)""",
            UUID.randomUUID(), c, UUID.randomUUID(), corr, now, now, level, category, event, barcode);
    }

    private void serverEvent(UUID c, String level, String event, UUID corr) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.server_events (id,occurred_at,level,event,correlation_id,contributor_id)
            VALUES (?,?,?,?,?,?)""", UUID.randomUUID(), now, level, event, corr, c);
    }
}
