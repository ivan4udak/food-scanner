package com.foodscanner.infrastructure.persistence;

import com.foodscanner.application.port.MeReadPort;
import com.foodscanner.application.port.MeReadPort.ScanData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слой: infrastructure (IT). Проверяет «Мои сканы» против реального Postgres,
 * включая изоляцию данных по пользователю.
 */
class MeReadAdapterIT extends AbstractRepositoryIT {

    @Autowired MeReadPort port;
    @Autowired JdbcTemplate jdbc;

    @Test
    void scansPhotosAndOwnership() {
        UUID me = contributor();
        UUID other = contributor();

        // Завершённый скан: draft + entry + 2 фото записи
        String doneBc = "done" + System.nanoTime();
        UUID doneDraft = draft(me, doneBc, "COMPLETED");
        UUID entry = entry(me, doneBc, doneDraft);
        entryPhoto(entry, "FRONT");
        entryPhoto(entry, "INGREDIENTS");

        // Открытый черновик: 1 фото черновика
        String openBc = "open" + System.nanoTime();
        UUID openDraft = draft(me, openBc, "OPEN");
        draftPhoto(openDraft, "FRONT");

        // список
        List<ScanData> scans = port.scans(me);
        assertThat(scans).hasSize(2);

        ScanData done = scans.stream().filter(s -> s.barcode().equals(doneBc)).findFirst().orElseThrow();
        assertThat(done.catalogEntryId()).isEqualTo(entry);
        assertThat(done.photoCount()).isEqualTo(2);

        ScanData open = scans.stream().filter(s -> s.barcode().equals(openBc)).findFirst().orElseThrow();
        assertThat(open.catalogEntryId()).isNull();
        assertThat(open.photoCount()).isEqualTo(1);

        // фото: из записи (2) и из черновика (1)
        assertThat(port.photos(me, doneBc)).hasSize(2);
        assertThat(port.photos(me, openBc)).hasSize(1);

        // изоляция: чужой пользователь не видит мой скан
        assertThat(port.scan(other, doneBc)).isEmpty();
        assertThat(port.scan(me, doneBc)).isPresent();
    }

    @Test
    void ocrForScanReturnsActiveJobsByDraftOrEntry() {
        UUID me = contributor();
        String bc = "ocr" + System.nanoTime();
        UUID d = draft(me, bc, "OPEN");
        ocrJob(d, null, "INGREDIENTS", (short) 2, true);   // active → виден
        ocrJob(d, null, "NUTRITION", (short) 0, true);     // active → виден
        ocrJob(d, null, "INGREDIENTS", (short) 5, false);  // inactive → скрыт

        List<MeReadPort.OcrData> ocr = port.ocrForScan(d, null);
        assertThat(ocr).hasSize(2);
        assertThat(ocr).extracting(MeReadPort.OcrData::photoType)
            .containsExactlyInAnyOrder("INGREDIENTS", "NUTRITION");

        // null draftId + null entryId → пусто (CAST(null) безопасен)
        assertThat(port.ocrForScan(null, null)).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────
    private void ocrJob(UUID draftId, UUID entryId, String type, short status, boolean active) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.ocr_jobs
              (id,draft_id,catalog_entry_id,storage_key,photo_type,status,attempts,active,created_at,updated_at)
            VALUES (?,?,?,?,?,?,1,?,?,?)""",
            UUID.randomUUID(), draftId, entryId, "photos/" + UUID.randomUUID(), type, status, active, now, now);
    }

    private UUID contributor() {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        String u = "u" + UUID.randomUUID();
        jdbc.update("""
            INSERT INTO food_catalog.contributors
              (id,nickname,username,password_hash,failed_login_attempts,completed_catalog_count,
               hidden_from_leaderboard,role,created_at,updated_at)
            VALUES (?,?,?,?,0,0,false,'USER',?,?)""", id, u, u, "h", now, now);
        return id;
    }

    private UUID draft(UUID c, String barcode, String status) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_drafts (id,barcode,contributor_id,status,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", id, barcode, c, status, now, now);
        return id;
    }

    private UUID entry(UUID c, String barcode, UUID draftId) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entries (id,barcode,contributor_id,draft_id,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", id, barcode, c, draftId, now, now);
        return id;
    }

    private void entryPhoto(UUID entryId, String type) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.catalog_entry_photos (id,entry_id,type,storage_key,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", UUID.randomUUID(), entryId, type, "photos/" + UUID.randomUUID(), now, now);
    }

    private void draftPhoto(UUID draftId, String type) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
            INSERT INTO food_catalog.draft_photos (id,draft_id,type,storage_key,created_at,updated_at)
            VALUES (?,?,?,?,?,?)""", UUID.randomUUID(), draftId, type, "photos/" + UUID.randomUUID(), now, now);
    }
}
