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

        // Брошенный черновик без фото — остаётся в БД, но не считается сканом.
        draft(ivan);

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

        // поиск по нику (для перехода из /stats)
        AdminUserRow byName = port.userByUsername(usernameOf(ivan), now.minus(5, ChronoUnit.MINUTES)).orElseThrow();
        assertThat(byName.id()).isEqualTo(ivan);
        assertThat(port.userByUsername("no-such-user", now)).isEmpty();
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

        // ошибки конкретного пользователя (только WARN/ERROR)
        List<AdminClientLog> ivanErrors = port.clientErrorsByUser(ivan, 50);
        assertThat(ivanErrors).hasSize(1);
        assertThat(ivanErrors.get(0).event()).isEqualTo("PHOTO_UPLOAD_FAILED");

        // trace
        assertThat(port.clientLogsByCorrelation(corr)).hasSize(1);
        assertThat(port.serverEventsByCorrelation(corr)).hasSize(1);

        // catalog
        String bc = barcodeStr2(entry);
        AdminCatalogRow cat = port.catalogByBarcode(bc).orElseThrow();
        assertThat(cat.author()).isEqualTo(usernameOf(ivan));
        assertThat(port.catalogPhotos(barcodeEntry)).hasSize(1);
    }

    @Test
    void presenceFromActivityEvenWithoutSession() {
        Instant now = Instant.now();
        UUID user = contributor("act_" + UUID.randomUUID());
        // Только heartbeat-активность, строки сессии нет (например, /client/session не дошёл)
        activity(user, now);

        AdminUserRow row = port.user(user, now.minus(5, ChronoUnit.MINUTES)).orElseThrow();
        assertThat(row.online()).isTrue();
        assertThat(row.lastActivityAt()).isNotNull();

        AdminDashboard d = port.dashboard(now.minus(1, ChronoUnit.HOURS),
            now.minus(7, ChronoUnit.DAYS), now.minus(5, ChronoUnit.MINUTES));
        assertThat(d.onlineNow()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void ocrJobsResolveBarcodeFilterAndSummary() {
        UUID ivan = contributor("ocr_" + UUID.randomUUID());
        UUID d = draft(ivan);
        String bc = barcodeStr(d);
        UUID jobId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.ocr_jobs
              (id,draft_id,storage_key,photo_type,status,attempts,raw_text,error_message,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?)""",
            jobId, d, "photos/" + jobId + ".jpg", "INGREDIENTS", (short) 2, 1,
            "Состав: вода, сахар", "stub", now, now);

        AdminOcrRow row = port.ocrJobs(null, null, false, false, 50, 0).stream()
            .filter(r -> r.jobId().equals(jobId)).findFirst().orElseThrow();
        assertThat(row.barcode()).isEqualTo(bc);              // резолв через draft
        assertThat(row.author()).isEqualTo(usernameOf(ivan)); // автор через draft.contributor
        assertThat(row.contributorId()).isEqualTo(ivan);
        assertThat(row.statusCode()).isEqualTo(2);
        assertThat(row.status()).isEqualTo("NEEDS_REVIEW");
        assertThat(row.active()).isTrue();
        assertThat(row.orphaned()).isFalse();
        assertThat(row.photoType()).isEqualTo("INGREDIENTS");
        assertThat(row.rawTextPreview()).contains("Состав");

        // блоки для карточек: по barcode и по пользователю
        assertThat(port.ocrJobsByBarcode(bc, 50)).anyMatch(r -> r.jobId().equals(jobId));
        assertThat(port.ocrJobsByUser(ivan, 50)).anyMatch(r -> r.jobId().equals(jobId));

        assertThat(port.ocrJobs(2, null, false, false, 50, 0)).anyMatch(r -> r.jobId().equals(jobId));
        assertThat(port.ocrJobs(5, null, false, false, 50, 0)).noneMatch(r -> r.jobId().equals(jobId));
        assertThat(port.ocrJobs(null, bc, false, false, 50, 0)).anyMatch(r -> r.jobId().equals(jobId));

        // inactive исключается по умолчанию, но виден при showInactive
        jdbc.update("UPDATE food_catalog.ocr_jobs SET active=false WHERE id=?", jobId);
        assertThat(port.ocrJobs(null, null, false, false, 50, 0)).noneMatch(r -> r.jobId().equals(jobId));
        assertThat(port.ocrJobs(null, null, true, false, 50, 0)).anyMatch(r -> r.jobId().equals(jobId));

        AdminOcrSummary sum = port.ocrSummary();
        assertThat(sum.byStatus()).hasSize(6);                // zero-fill всех статусов
        assertThat(sum.queueSize()).isGreaterThanOrEqualTo(0);
    }

    private void activity(UUID c, Instant at) {
        Timestamp ts = Timestamp.from(at);
        jdbc.update("""
            INSERT INTO food_catalog.client_activity
              (id,contributor_id,session_id,screen,online,occurred_at,received_at)
            VALUES (?,?,?,?,?,?,?)""",
            UUID.randomUUID(), c, UUID.randomUUID(), "ScannerPage", true, ts, ts);
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
