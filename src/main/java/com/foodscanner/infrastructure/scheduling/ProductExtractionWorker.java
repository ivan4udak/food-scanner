package com.foodscanner.infrastructure.scheduling;

import com.foodscanner.application.port.ProductExtractor;
import com.foodscanner.domain.model.extraction.ExtractionResult;
import com.foodscanner.domain.model.extraction.ExtractionStatus;
import com.foodscanner.domain.model.extraction.ProductExtractionJob;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.policy.ProcessingWindow;
import com.foodscanner.domain.repository.OcrJobRepository;
import com.foodscanner.domain.repository.ProductExtractionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Слой: infrastructure.
 * v1.12.1 — ночной батч извлечения: только в окне, concurrency=1 (один прогон за раз),
 * лимиты на окно (jobs/минуты), runtime-safety (heap). Вне окна/при перегрузке — задачи остаются QUEUED.
 * Реальный extractor подключается флагом product.extractor (сейчас Stub → SKIPPED).
 */
@Component
public class ProductExtractionWorker {

    private static final Logger log = LoggerFactory.getLogger(ProductExtractionWorker.class);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ProductExtractionJobRepository repository;
    private final ProductExtractor extractor;
    private final OcrJobRepository ocrJobs;

    private final boolean enabled;
    private final ProcessingWindow window;
    private final ZoneId zone;
    private final int maxJobs;
    private final long maxMillis;

    public ProductExtractionWorker(
            ProductExtractionJobRepository repository, ProductExtractor extractor, OcrJobRepository ocrJobs,
            @Value("${product.extractor.worker.enabled:true}") boolean enabled,
            @Value("${product.extractor.worker.window-start:00:00}") String windowStart,
            @Value("${product.extractor.worker.window-end:06:00}") String windowEnd,
            @Value("${product.extractor.worker.timezone:Europe/Moscow}") String timezone,
            @Value("${product.extractor.worker.max-jobs-per-window:50}") int maxJobs,
            @Value("${product.extractor.worker.max-minutes-per-window:240}") long maxMinutes) {
        this.repository = repository;
        this.extractor = extractor;
        this.ocrJobs = ocrJobs;
        this.enabled = enabled;
        this.window = ProcessingWindow.of(windowStart, windowEnd);
        this.zone = ZoneId.of(timezone);
        this.maxJobs = maxJobs;
        this.maxMillis = maxMinutes * 60_000L;
    }

    @Scheduled(fixedRateString = "${product.extractor.worker.interval-ms:300000}",
               initialDelayString = "${product.extractor.worker.initial-delay-ms:60000}")
    public void run() {
        if (!enabled) return;
        if (!window.contains(LocalTime.now(zone))) return;       // только в ночном окне
        if (!running.compareAndSet(false, true)) return;          // concurrency = 1
        try {
            if (!safeToRun()) {
                log.warn("extraction worker: пропуск — недостаточно heap (оставляем QUEUED)");
                return;
            }
            long deadline = System.currentTimeMillis() + maxMillis;
            List<ProductExtractionJob> batch = repository.findQueued(maxJobs);
            int ok = 0, skipped = 0, failed = 0;
            for (ProductExtractionJob job : batch) {
                if (System.currentTimeMillis() > deadline) break;
                try {
                    repository.markInProgress(job.id());
                    String rawText = ocrJobs.findById(job.ocrJobId()).map(OcrJob::rawText).orElse(null);
                    ExtractionResult r = extractor.extract(job, rawText);
                    ExtractionStatus status = !r.hasAny() ? ExtractionStatus.SKIPPED
                        : r.needsReview() ? ExtractionStatus.NEEDS_REVIEW : ExtractionStatus.STRUCTURED;
                    repository.applyResult(job.id(), status, r);
                    if (status == ExtractionStatus.SKIPPED) skipped++; else ok++;
                } catch (Exception e) {
                    repository.markFailed(job.id(), e.getMessage());
                    failed++;
                }
            }
            if (ok + skipped + failed > 0) {
                log.info("extraction worker: structured={}, skipped={}, failed={}", ok, skipped, failed);
            }
        } finally {
            running.set(false);
        }
    }

    /** Базовая защита: не запускаемся при заполненной куче (>90%). */
    private boolean safeToRun() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        return (double) used / rt.maxMemory() < 0.90;
    }
}
