package com.foodscanner.application.service;

import com.foodscanner.application.port.OcrJobPublisher;
import com.foodscanner.application.usecase.ReprocessOcrUseCase;
import com.foodscanner.domain.model.ocr.OcrJob;
import com.foodscanner.domain.model.ocr.OcrStatus;
import com.foodscanner.domain.repository.OcrJobRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Слой: application.
 * Reprocess: создаёт новую QUEUED-задачу для того же фото, замещает старую и публикует.
 * Разрешено только для PHOTO_UNREADABLE(3)/ERROR(5) — когда оператор считает, что фото читаемо.
 */
@Service
public class ReprocessOcrService implements ReprocessOcrUseCase {

    private final OcrJobRepository repository;
    private final OcrJobPublisher publisher;

    public ReprocessOcrService(OcrJobRepository repository, OcrJobPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public Optional<UUID> execute(UUID jobId) {
        return repository.findById(jobId).map(old -> {
            int code = old.status().code();
            if (code != OcrStatus.PHOTO_UNREADABLE.code() && code != OcrStatus.ERROR.code()) {
                throw new IllegalStateException(
                    "reprocess только для PHOTO_UNREADABLE(3)/ERROR(5), текущий статус=" + code);
            }
            OcrJob fresh = repository.save(
                OcrJob.requeue(old.draftId(), old.catalogEntryId(), old.storageKey(), old.photoType()));
            repository.supersede(old.id(), fresh.id());
            if (publisher.publish(fresh)) {
                repository.markPublished(fresh.id());
            }
            return fresh.id();
        });
    }
}
