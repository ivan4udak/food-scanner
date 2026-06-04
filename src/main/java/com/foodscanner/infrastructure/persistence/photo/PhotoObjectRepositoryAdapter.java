package com.foodscanner.infrastructure.persistence.photo;

import com.foodscanner.domain.repository.PhotoObjectRepository;
import com.foodscanner.infrastructure.persistence.draft.DraftPhotoJpaRepository;
import com.foodscanner.infrastructure.persistence.entry.CatalogEntryPhotoJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PhotoObjectRepositoryAdapter implements PhotoObjectRepository {

    private final PhotoObjectJpaRepository        objects;
    private final DraftPhotoJpaRepository         draftPhotos;
    private final CatalogEntryPhotoJpaRepository  entryPhotos;

    public PhotoObjectRepositoryAdapter(PhotoObjectJpaRepository objects,
                                        DraftPhotoJpaRepository draftPhotos,
                                        CatalogEntryPhotoJpaRepository entryPhotos) {
        this.objects     = objects;
        this.draftPhotos = draftPhotos;
        this.entryPhotos = entryPhotos;
    }

    @Override
    public Optional<String> findObjectKeyByHash(String hash) {
        return objects.findById(hash).map(PhotoObjectJpaEntity::getObjectKey);
    }

    @Override
    public void register(String hash, String objectKey) {
        objects.save(new PhotoObjectJpaEntity(hash, objectKey, Instant.now()));
    }

    @Override
    public boolean isObjectKeyReferenced(String objectKey) {
        return draftPhotos.countByStorageKey(objectKey) > 0
            || entryPhotos.countByStorageKey(objectKey) > 0;
    }

    @Override
    @Transactional
    public void deleteByObjectKey(String objectKey) {
        objects.deleteByObjectKey(objectKey);
    }
}
