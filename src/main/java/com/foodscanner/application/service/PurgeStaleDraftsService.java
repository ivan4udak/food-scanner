package com.foodscanner.application.service;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.result.PurgeResult;
import com.foodscanner.application.usecase.PurgeStaleDraftsUseCase;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.DraftPhoto;
import com.foodscanner.domain.repository.CatalogDraftRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Слой: application
 * Удаляет незавершённые черновики старше maxAge: объекты в хранилище (full + thumbnail),
 * затем сам черновик (draft_photos — каскадно). Best-effort по объектам:
 * ошибка удаления одного не останавливает очистку остальных.
 */
public class PurgeStaleDraftsService implements PurgeStaleDraftsUseCase {

    private final CatalogDraftRepository draftRepository;
    private final PhotoStorage           photoStorage;

    public PurgeStaleDraftsService(CatalogDraftRepository draftRepository, PhotoStorage photoStorage) {
        this.draftRepository = draftRepository;
        this.photoStorage    = photoStorage;
    }

    @Override
    public PurgeResult purge(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        List<CatalogDraft> stale = draftRepository.findStaleUnfinished(cutoff);

        int objects = 0;
        for (CatalogDraft draft : stale) {
            for (DraftPhoto photo : draft.getPhotos()) {
                objects += deleteQuietly(photo.getStorageKey());
                objects += deleteQuietly(thumbKey(photo.getStorageKey()));
            }
            draftRepository.deleteById(draft.getId());
        }
        return new PurgeResult(stale.size(), objects);
    }

    private int deleteQuietly(String key) {
        try {
            photoStorage.delete(key);
            return 1;
        } catch (RuntimeException e) {
            return 0;   // объект мог отсутствовать — не валим очистку
        }
    }

    private static String thumbKey(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? key + "_thumb" : key.substring(0, dot) + "_thumb" + key.substring(dot);
    }
}
