package com.foodscanner.application.service;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.result.PurgeResult;
import com.foodscanner.application.usecase.PurgeStaleDraftsUseCase;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.DraftPhoto;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.PhotoObjectRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Слой: application
 * Удаляет незавершённые черновики старше maxAge. С учётом дедупликации (Блок 16):
 * объект в хранилище удаляется только если на него больше нет ссылок
 * (ни из draft_photos, ни из catalog_entry_photos). Best-effort по объектам.
 */
public class PurgeStaleDraftsService implements PurgeStaleDraftsUseCase {

    private final CatalogDraftRepository draftRepository;
    private final PhotoStorage           photoStorage;
    private final PhotoObjectRepository  photoObjects;

    public PurgeStaleDraftsService(CatalogDraftRepository draftRepository,
                                   PhotoStorage photoStorage,
                                   PhotoObjectRepository photoObjects) {
        this.draftRepository = draftRepository;
        this.photoStorage    = photoStorage;
        this.photoObjects    = photoObjects;
    }

    @Override
    public PurgeResult purge(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        List<CatalogDraft> stale = draftRepository.findStaleUnfinished(cutoff);

        int objects = 0;
        for (CatalogDraft draft : stale) {
            Set<String> keys = new LinkedHashSet<>();
            for (DraftPhoto p : draft.getPhotos()) keys.add(p.getStorageKey());

            draftRepository.deleteById(draft.getId());   // удаляет draft_photos (каскад)

            for (String key : keys) {
                if (photoObjects.isObjectKeyReferenced(key)) continue;  // объект ещё используется
                objects += deleteQuietly(key);
                objects += deleteQuietly(DeduplicatingPhotoStore.thumbKey(key));
                photoObjects.deleteByObjectKey(key);
            }
        }
        return new PurgeResult(stale.size(), objects);
    }

    private int deleteQuietly(String key) {
        try {
            photoStorage.delete(key);
            return 1;
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
