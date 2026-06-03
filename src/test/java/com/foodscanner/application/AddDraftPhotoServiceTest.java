package com.foodscanner.application;

import com.foodscanner.application.command.AddDraftPhotoCommand;
import com.foodscanner.application.result.AddDraftPhotoResult;
import com.foodscanner.application.service.AddDraftPhotoService;
import com.foodscanner.domain.exception.CatalogDraftNotFoundException;
import com.foodscanner.domain.model.*;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Слой: application
 *
 * Тестирует AddDraftPhotoService:
 * - фото добавляется к существующему черновику
 * - прогресс возвращается корректно (0/6 → 6/6)
 * - ошибка если черновик не найден
 * - ошибка если чужой черновик
 */
@DisplayName("AddDraftPhotoUseCase")
class AddDraftPhotoServiceTest {

    private CatalogDraftRepository  draftRepository;
    private CatalogCompletionPolicy policy;
    private AddDraftPhotoService    service;

    private static final UUID CONTRIBUTOR_ID = UUID.randomUUID();
    private static final UUID OTHER_ID       = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        draftRepository = mock(CatalogDraftRepository.class);
        policy          = new CatalogCompletionPolicy();
        service         = new AddDraftPhotoService(draftRepository, policy);
    }

    private CatalogDraft openDraft() {
        return CatalogDraft.create(new Barcode("4607038310042"), CONTRIBUTOR_ID);
    }

    @Nested
    @DisplayName("Успешное добавление фото")
    class Success {

        @Test
        @DisplayName("Добавляет фото и возвращает прогресс 1/6")
        void shouldAddFirstPhotoAndReturnProgress() {
            CatalogDraft draft = openDraft();
            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AddDraftPhotoResult result = service.execute(new AddDraftPhotoCommand(
                draft.getId(), CONTRIBUTOR_ID, PhotoType.FRONT, "drafts/123/front.jpg"));

            assertEquals(1, result.getUploadedCount());
            assertEquals(6, result.getRequiredCount());
            assertFalse(result.isComplete());
        }

        @Test
        @DisplayName("Возвращает isComplete=true после 6 обязательных фото")
        void shouldReturnCompleteAfterAllPhotos() {
            CatalogDraft draft = openDraft();
            draft.addPhoto(PhotoType.BARCODE,     "b.jpg");
            draft.addPhoto(PhotoType.FRONT,       "f.jpg");
            draft.addPhoto(PhotoType.BACK,        "ba.jpg");
            draft.addPhoto(PhotoType.INGREDIENTS, "i.jpg");
            draft.addPhoto(PhotoType.NUTRITION,   "n.jpg");

            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AddDraftPhotoResult result = service.execute(new AddDraftPhotoCommand(
                draft.getId(), CONTRIBUTOR_ID, PhotoType.EXTRA, "e.jpg"));

            assertTrue(result.isComplete());
            assertEquals(6, result.getUploadedCount());
        }

        @Test
        @DisplayName("Сохраняет обновлённый черновик")
        void shouldSaveDraftAfterAddingPhoto() {
            CatalogDraft draft = openDraft();
            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new AddDraftPhotoCommand(
                draft.getId(), CONTRIBUTOR_ID, PhotoType.FRONT, "f.jpg"));

            verify(draftRepository).save(draft);
        }

        @Test
        @DisplayName("missingTypes не содержит уже загруженные типы")
        void shouldExcludeUploadedTypesFromMissing() {
            CatalogDraft draft = openDraft();
            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AddDraftPhotoResult result = service.execute(new AddDraftPhotoCommand(
                draft.getId(), CONTRIBUTOR_ID, PhotoType.FRONT, "f.jpg"));

            assertFalse(result.getMissingTypes().contains(PhotoType.FRONT));
        }
    }

    @Nested
    @DisplayName("Ошибки")
    class ErrorCases {

        @Test
        @DisplayName("Бросает исключение если черновик не найден")
        void shouldThrowWhenDraftNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(draftRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(CatalogDraftNotFoundException.class,
                () -> service.execute(new AddDraftPhotoCommand(
                    unknownId, CONTRIBUTOR_ID, PhotoType.FRONT, "f.jpg")));
        }

        @Test
        @DisplayName("Бросает исключение если черновик принадлежит другому контрибьютору")
        void shouldThrowWhenDraftBelongsToOtherContributor() {
            CatalogDraft draft = openDraft(); // создан с CONTRIBUTOR_ID
            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThrows(IllegalStateException.class,
                () -> service.execute(new AddDraftPhotoCommand(
                    draft.getId(), OTHER_ID, PhotoType.FRONT, "f.jpg")));
        }
    }
}
