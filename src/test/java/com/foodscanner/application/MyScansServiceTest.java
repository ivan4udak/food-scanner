package com.foodscanner.application;

import com.foodscanner.application.port.MeReadPort;
import com.foodscanner.application.port.MeReadPort.PhotoData;
import com.foodscanner.application.port.MeReadPort.ScanData;
import com.foodscanner.application.result.me.MeScanDetail;
import com.foodscanner.application.result.me.MeScanRow;
import com.foodscanner.application.service.MyScansService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MyScansServiceTest {

    private final MeReadPort port = mock(MeReadPort.class);
    private final MyScansService service = new MyScansService(port);

    private static final UUID ME = UUID.randomUUID();

    @Test
    void mapsStatusCompletedAndDraftOpen() {
        UUID entryId = UUID.randomUUID();
        when(port.scans(ME)).thenReturn(List.of(
            new ScanData("460", "COMPLETED", UUID.randomUUID(), entryId, Instant.now(), Instant.now(), 4),
            new ScanData("777", "OPEN", UUID.randomUUID(), null, Instant.now(), null, 1)));

        List<MeScanRow> rows = service.list(ME);

        assertThat(rows.get(0).scanStatus()).isEqualTo("COMPLETED");
        assertThat(rows.get(0).catalogEntryId()).isEqualTo(entryId);
        assertThat(rows.get(1).scanStatus()).isEqualTo("DRAFT_OPEN");
        assertThat(rows.get(0).ocrStatus()).isNull(); // задел под OCR, пока null
    }

    @Test
    void detailBuildsThumbAndFullUrls() {
        when(port.scan(ME, "460")).thenReturn(Optional.of(
            new ScanData("460", "COMPLETED", null, UUID.randomUUID(), Instant.now(), Instant.now(), 1)));
        when(port.photos(ME, "460")).thenReturn(List.of(
            new PhotoData(UUID.randomUUID(), "FRONT", "photos/hash.jpg", Instant.now())));

        MeScanDetail detail = service.detail(ME, "460").orElseThrow();

        assertThat(detail.photos()).hasSize(1);
        MeScanDetail.Photo p = detail.photos().get(0);
        assertThat(p.thumbUrl()).isEqualTo("/api/v1/photos/photos/hash.jpg?size=thumb");
        assertThat(p.fullUrl()).isEqualTo("/api/v1/photos/photos/hash.jpg?size=full");
        assertThat(detail.ocr()).isEmpty(); // нет OCR — пустой список, не null
    }

    @Test
    void detailIncludesOcrJobsWithStatusName() {
        when(port.scan(ME, "460")).thenReturn(Optional.of(
            new ScanData("460", "COMPLETED", null, UUID.randomUUID(), Instant.now(), Instant.now(), 1)));
        when(port.photos(ME, "460")).thenReturn(List.of());
        when(port.ocrForScan(any(), any())).thenReturn(List.of(
            new MeReadPort.OcrData("INGREDIENTS", 2, null, Instant.now(), "Состав…", null, "stub")));

        MeScanDetail detail = service.detail(ME, "460").orElseThrow();

        assertThat(detail.ocr()).hasSize(1);
        assertThat(detail.ocr().get(0).photoType()).isEqualTo("INGREDIENTS");
        assertThat(detail.ocr().get(0).statusCode()).isEqualTo(2);
        assertThat(detail.ocr().get(0).status()).isEqualTo("NEEDS_REVIEW");
    }

    @Test
    void detailEmptyWhenNotOwnedOrMissing() {
        when(port.scan(eq(ME), any())).thenReturn(Optional.empty());
        assertThat(service.detail(ME, "000")).isEmpty();
        verify(port, never()).photos(any(), any());
    }
}
