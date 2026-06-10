package com.foodscanner.infrastructure.scheduling;

import com.foodscanner.application.result.PurgeResult;
import com.foodscanner.application.usecase.PurgeStaleDraftsUseCase;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Слой: infrastructure
 * Блок 15: раз в час удаляет незавершённые черновики старше N часов
 * (draft + draft_photos + объекты MinIO). Логирует количество удалённого.
 */
@Component
public class StaleDraftCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(StaleDraftCleanupJob.class);

    private final PurgeStaleDraftsUseCase purge;
    private final OcrJobRepository ocrJobs;
    private final long ttlHours;

    public StaleDraftCleanupJob(PurgeStaleDraftsUseCase purge, OcrJobRepository ocrJobs,
                                @Value("${cleanup.draft-ttl-hours:24}") long ttlHours) {
        this.purge    = purge;
        this.ocrJobs  = ocrJobs;
        this.ttlHours = ttlHours;
    }

    @Scheduled(fixedRateString = "${cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        PurgeResult r = purge.purge(Duration.ofHours(ttlHours));
        // OCR-задачи удалённых черновиков (без catalog_entry) помечаем orphan — не висят как active
        int orphaned = ocrJobs.markOrphans();
        if (r.draftsDeleted() > 0 || r.objectsDeleted() > 0 || orphaned > 0) {
            log.info("Очистка мусора: удалено черновиков={}, объектов хранилища={}, OCR-orphan={}",
                r.draftsDeleted(), r.objectsDeleted(), orphaned);
        }
    }
}
