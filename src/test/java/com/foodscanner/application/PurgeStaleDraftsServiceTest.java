package com.foodscanner.application;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.result.PurgeResult;
import com.foodscanner.application.service.PurgeStaleDraftsService;
import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.PhotoType;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.PhotoObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PurgeStaleDraftsService — очистка мусора (hash-aware)")
class PurgeStaleDraftsServiceTest {

    private CatalogDraftRepository draftRepo;
    private PhotoStorage photoStorage;
    private PhotoObjectRepository photoObjects;
    private PurgeStaleDraftsService service;

    @BeforeEach
    void setUp() {
        draftRepo = mock(CatalogDraftRepository.class);
        photoStorage = mock(PhotoStorage.class);
        photoObjects = mock(PhotoObjectRepository.class);
        service = new PurgeStaleDraftsService(draftRepo, photoStorage, photoObjects);
    }

    private CatalogDraft staleDraftWithTwoPhotos() {
        CatalogDraft d = CatalogDraft.create(new Barcode("4607038310042"), UUID.randomUUID());
        d.addPhoto(PhotoType.BARCODE, "photos/aaa.jpg");
        d.addPhoto(PhotoType.FRONT,   "photos/bbb.jpg");
        return d;
    }

    @Test
    @DisplayName("несвязанные объекты удаляются (full+thumb) + draft + запись реестра")
    void purgesUnreferenced() {
        CatalogDraft d = staleDraftWithTwoPhotos();
        when(draftRepo.findStaleUnfinished(any())).thenReturn(List.of(d));
        when(photoObjects.isObjectKeyReferenced(any())).thenReturn(false);

        PurgeResult r = service.purge(Duration.ofHours(24));

        assertEquals(1, r.draftsDeleted());
        assertEquals(4, r.objectsDeleted());                 // 2 объекта × (full+thumb)
        verify(draftRepo).deleteById(d.getId());
        verify(photoStorage).delete("photos/aaa.jpg");
        verify(photoStorage).delete("photos/aaa_thumb.jpg");
        verify(photoObjects).deleteByObjectKey("photos/aaa.jpg");
        verify(photoObjects).deleteByObjectKey("photos/bbb.jpg");
    }

    @Test
    @DisplayName("общий объект (есть ссылки) НЕ удаляется")
    void keepsReferenced() {
        CatalogDraft d = staleDraftWithTwoPhotos();
        when(draftRepo.findStaleUnfinished(any())).thenReturn(List.of(d));
        when(photoObjects.isObjectKeyReferenced(any())).thenReturn(true);  // ещё используется

        PurgeResult r = service.purge(Duration.ofHours(24));

        assertEquals(1, r.draftsDeleted());
        assertEquals(0, r.objectsDeleted());
        verify(draftRepo).deleteById(d.getId());
        verify(photoStorage, never()).delete(any());
        verify(photoObjects, never()).deleteByObjectKey(any());
    }

    @Test
    @DisplayName("нет устаревших — ничего не делаем")
    void nothingToPurge() {
        when(draftRepo.findStaleUnfinished(any())).thenReturn(List.of());
        PurgeResult r = service.purge(Duration.ofHours(24));
        assertEquals(0, r.draftsDeleted());
        assertEquals(0, r.objectsDeleted());
        verify(draftRepo, never()).deleteById(any());
        verifyNoInteractions(photoStorage);
    }
}
