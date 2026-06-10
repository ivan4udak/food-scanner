package com.foodscanner.application.service;

import com.foodscanner.application.port.MeReadPort;
import com.foodscanner.application.port.MeReadPort.PhotoData;
import com.foodscanner.application.port.MeReadPort.ScanData;
import com.foodscanner.application.result.me.MeScanDetail;
import com.foodscanner.application.result.me.MeScanOcr;
import com.foodscanner.application.result.me.MeScanRow;
import com.foodscanner.application.usecase.MyScansUseCase;
import com.foodscanner.domain.model.ocr.OcrStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * «Мои сканы»: маппинг статуса скана и сборка URL фото (thumb/full).
 */
@Service
public class MyScansService implements MyScansUseCase {

    private final MeReadPort port;

    public MyScansService(MeReadPort port) {
        this.port = port;
    }

    @Override
    public List<MeScanRow> list(UUID contributorId) {
        return port.scans(contributorId).stream().map(MyScansService::toRow).toList();
    }

    @Override
    public Optional<MeScanDetail> detail(UUID contributorId, String barcode) {
        return port.scan(contributorId, barcode).map(s -> {
            List<MeScanDetail.Photo> photos = port.photos(contributorId, barcode).stream()
                .map(MyScansService::toPhoto).toList();
            List<MeScanOcr> ocr = port.ocrForScan(s.draftId(), s.catalogEntryId()).stream()
                .map(MyScansService::toOcr).toList();
            return new MeScanDetail(s.barcode(), s.catalogEntryId(),
                s.firstScannedAt(), s.completedAt(), photos, ocr, null);
        });
    }

    private static MeScanRow toRow(ScanData s) {
        return new MeScanRow(s.barcode(), status(s), s.catalogEntryId(),
            s.firstScannedAt(), s.completedAt(), s.photoCount(), null);
    }

    /** Завершён → COMPLETED; иначе OPEN → DRAFT_OPEN; прочее — как в БД. */
    private static String status(ScanData s) {
        if (s.catalogEntryId() != null) return "COMPLETED";
        return "OPEN".equalsIgnoreCase(s.status()) ? "DRAFT_OPEN" : s.status();
    }

    private static MeScanOcr toOcr(MeReadPort.OcrData o) {
        return new MeScanOcr(o.photoType(), o.statusCode(), OcrStatus.fromCode(o.statusCode()).name(),
            o.confidence(), o.updatedAt(), o.errorCode(), o.errorMessage(), o.rawTextPreview());
    }

    private static MeScanDetail.Photo toPhoto(PhotoData p) {
        String base = "/api/v1/photos/" + p.storageKey();
        return new MeScanDetail.Photo(p.id(), p.type(), p.storageKey(),
            base + "?size=thumb", base + "?size=full", p.capturedAt());
    }
}
