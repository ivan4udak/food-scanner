package com.foodscanner.application;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.result.PurgeResult;
import com.foodscanner.application.service.PurgeStaleDraftsService;
import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.PhotoType;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PurgeStaleDraftsService — очистка мусора")
class PurgeStaleDraftsServiceTest {

    private CatalogDraftRepository draftRepo;
    private PhotoStorage photoStorage;
    private PurgeStaleDraftsService service;

    @BeforeEach
    void setUp() {
        draftRepo = mock(CatalogDraftRepository.class);
        photoStorage = mock(PhotoStorage.class);
        service = new PurgeStaleDraftsService(draftRepo, photoStorage);
    }

    private CatalogDraft staleDraftWithTwoPhotos() {
        CatalogDraft d = CatalogDraft.create(new Barcode("4607038310042"), UUID.randomUUID());
        d.addPhoto(PhotoType.BARCODE, "drafts/x/barcode/u.jpg");
        d.addPhoto(PhotoType.FRONT,   "drafts/x/front/u.jpg");
        return d;
    }

    @Test
    @DisplayName("удаляет объекты (full+thumb) и сам черновик, считает количество")
    void purgesDraftAndObjects() {
        CatalogDraft d = staleDraftWithTwoPhotos();
        when(draftRepo.findStaleUnfinished(any())).thenReturn(List.of(d));

        PurgeResult r = service.purge(Duration.ofHours(24));

        assertEquals(1, r.draftsDeleted());
        assertEquals(4, r.objectsDeleted());                 // 2 фото × (full + thumb)
        verify(photoStorage).delete("drafts/x/barcode/u.jpg");
        verify(photoStorage).delete("drafts/x/barcode/u_thumb.jpg");
        verify(photoStorage).delete("drafts/x/front/u.jpg");
        verify(photoStorage).delete("drafts/x/front/u_thumb.jpg");
        verify(draftRepo).deleteById(d.getId());
    }

    @Test
    @DisplayName("ошибка удаления объекта не останавливает очистку (best-effort)")
    void continuesOnStorageError() {
        CatalogDraft d = staleDraftWithTwoPhotos();
        when(draftRepo.findStaleUnfinished(any())).thenReturn(List.of(d));
        doThrow(new RuntimeException("missing")).when(photoStorage).delete("drafts/x/barcode/u.jpg");

        PurgeResult r = service.purge(Duration.ofHours(24));

        assertEquals(1, r.draftsDeleted());
        assertEquals(3, r.objectsDeleted());                 // один объект не удалился
        verify(draftRepo).deleteById(d.getId());
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
