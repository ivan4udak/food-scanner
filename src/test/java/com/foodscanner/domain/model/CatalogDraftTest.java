package com.foodscanner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Слой: domain
 *
 * Тестирует инварианты Aggregate Root CatalogDraft:
 * - создание только через фабричный метод
 * - статус при создании всегда OPEN
 * - управление фотографиями: добавление, замена, защита коллекции
 * - запрет добавления фото в COMPLETED / ABANDONED
 * - переходы статусов
 * - updatedAt обновляется при каждом изменении
 */
@DisplayName("CatalogDraft Aggregate Root")
class CatalogDraftTest {

    private static final UUID CONTRIBUTOR_ID = UUID.randomUUID();

    private CatalogDraft validDraft() {
        return CatalogDraft.create(new Barcode("4607038310042"), CONTRIBUTOR_ID);
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Создание")
    class Creation {

        @Test
        @DisplayName("Создаётся с валидными данными")
        void shouldCreateWithValidData() {
            CatalogDraft draft = validDraft();

            assertNotNull(draft.getId());
            assertEquals("4607038310042", draft.getBarcode().getValue());
            assertEquals(CONTRIBUTOR_ID, draft.getContributorId());
            assertEquals(CatalogDraftStatus.OPEN, draft.getStatus());
        }

        @Test
        @DisplayName("Список фотографий пустой при создании")
        void shouldHaveEmptyPhotosOnCreation() {
            assertTrue(validDraft().getPhotos().isEmpty());
        }

        @Test
        @DisplayName("updatedAt равен createdAt при создании")
        void shouldHaveUpdatedAtEqualToCreatedAt() {
            CatalogDraft draft = validDraft();
            assertEquals(draft.getCreatedAt(), draft.getUpdatedAt());
        }

        @Test
        @DisplayName("Отклоняет null barcode")
        void shouldRejectNullBarcode() {
            assertThrows(IllegalArgumentException.class,
                () -> CatalogDraft.create(null, CONTRIBUTOR_ID));
        }

        @Test
        @DisplayName("Отклоняет null contributorId")
        void shouldRejectNullContributorId() {
            assertThrows(IllegalArgumentException.class,
                () -> CatalogDraft.create(new Barcode("4607038310042"), null));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Управление фотографиями")
    class PhotoManagement {

        @Test
        @DisplayName("Добавляет фото в OPEN черновик")
        void shouldAddPhotoToOpenDraft() {
            CatalogDraft draft = validDraft();

            draft.addPhoto(PhotoType.FRONT, "drafts/123/front.jpg");

            assertEquals(1, draft.getPhotos().size());
            assertEquals(PhotoType.FRONT, draft.getPhotos().get(0).getType());
            assertEquals("drafts/123/front.jpg", draft.getPhotos().get(0).getStorageKey());
        }

        @Test
        @DisplayName("Можно добавить все пять обязательных типов")
        void shouldAddAllRequiredTypes() {
            CatalogDraft draft = validDraft();

            draft.addPhoto(PhotoType.BARCODE,     "drafts/123/barcode.jpg");
            draft.addPhoto(PhotoType.FRONT,       "drafts/123/front.jpg");
            draft.addPhoto(PhotoType.BACK,        "drafts/123/back.jpg");
            draft.addPhoto(PhotoType.INGREDIENTS, "drafts/123/ingredients.jpg");
            draft.addPhoto(PhotoType.NUTRITION,   "drafts/123/nutrition.jpg");

            assertEquals(5, draft.getPhotos().size());
        }

        @Test
        @DisplayName("Можно добавить несколько фото одного типа (перефотографировал)")
        void shouldAllowMultiplePhotosOfSameType() {
            CatalogDraft draft = validDraft();

            draft.addPhoto(PhotoType.FRONT, "drafts/123/front_v1.jpg");
            draft.addPhoto(PhotoType.FRONT, "drafts/123/front_v2.jpg");

            assertEquals(2, draft.getPhotos().size());
        }

        @Test
        @DisplayName("OTHER фото можно добавить опционально")
        void shouldAllowOptionalOtherPhoto() {
            CatalogDraft draft = validDraft();

            assertDoesNotThrow(() -> draft.addPhoto(PhotoType.EXTRA, "drafts/123/other.jpg"));
        }

        @Test
        @DisplayName("addPhoto обновляет updatedAt")
        void shouldUpdateUpdatedAtOnAddPhoto() throws InterruptedException {
            CatalogDraft draft = validDraft();
            Thread.sleep(2);

            draft.addPhoto(PhotoType.FRONT, "drafts/123/front.jpg");

            assertTrue(draft.getUpdatedAt().isAfter(draft.getCreatedAt()));
        }

        @Test
        @DisplayName("addPhoto отклоняет null type")
        void shouldRejectNullPhotoType() {
            assertThrows(IllegalArgumentException.class,
                () -> validDraft().addPhoto(null, "drafts/123/front.jpg"));
        }

        @Test
        @DisplayName("addPhoto отклоняет null storageKey")
        void shouldRejectNullStorageKey() {
            assertThrows(IllegalArgumentException.class,
                () -> validDraft().addPhoto(PhotoType.FRONT, null));
        }

        @Test
        @DisplayName("addPhoto отклоняет blank storageKey")
        void shouldRejectBlankStorageKey() {
            assertThrows(IllegalArgumentException.class,
                () -> validDraft().addPhoto(PhotoType.FRONT, "  "));
        }

        @Test
        @DisplayName("getPhotos возвращает unmodifiable список")
        void shouldReturnUnmodifiablePhotos() {
            CatalogDraft draft = validDraft();
            draft.addPhoto(PhotoType.FRONT, "drafts/123/front.jpg");

            assertThrows(UnsupportedOperationException.class,
                () -> draft.getPhotos().clear());
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Запрет добавления фото в завершённый / заброшенный черновик")
    class PhotoAdditionGuards {

        @Test
        @DisplayName("Нельзя добавить фото в COMPLETED черновик")
        void shouldNotAddPhotoToCompletedDraft() {
            CatalogDraft draft = draftWithAllRequiredPhotos();
            draft.markCompleted();

            assertThrows(IllegalStateException.class,
                () -> draft.addPhoto(PhotoType.EXTRA, "drafts/123/other.jpg"));
        }

        @Test
        @DisplayName("Нельзя добавить фото в ABANDONED черновик")
        void shouldNotAddPhotoToAbandonedDraft() {
            CatalogDraft draft = validDraft();
            draft.abandon();

            assertThrows(IllegalStateException.class,
                () -> draft.addPhoto(PhotoType.FRONT, "drafts/123/front.jpg"));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Переходы статусов")
    class StatusTransitions {

        @Test
        @DisplayName("OPEN → COMPLETED через markCompleted()")
        void shouldTransitionToCompleted() {
            CatalogDraft draft = draftWithAllRequiredPhotos();

            draft.markCompleted();

            assertEquals(CatalogDraftStatus.COMPLETED, draft.getStatus());
        }

        @Test
        @DisplayName("markCompleted() обновляет updatedAt")
        void shouldUpdateUpdatedAtOnComplete() throws InterruptedException {
            CatalogDraft draft = draftWithAllRequiredPhotos();
            Thread.sleep(2);

            draft.markCompleted();

            assertTrue(draft.getUpdatedAt().isAfter(draft.getCreatedAt()));
        }

        @Test
        @DisplayName("OPEN → ABANDONED через abandon()")
        void shouldTransitionToAbandoned() {
            CatalogDraft draft = validDraft();

            draft.abandon();

            assertEquals(CatalogDraftStatus.ABANDONED, draft.getStatus());
        }

        @Test
        @DisplayName("abandon() обновляет updatedAt")
        void shouldUpdateUpdatedAtOnAbandon() throws InterruptedException {
            CatalogDraft draft = validDraft();
            Thread.sleep(2);

            draft.abandon();

            assertTrue(draft.getUpdatedAt().isAfter(draft.getCreatedAt()));
        }

        @Test
        @DisplayName("COMPLETED нельзя перевести в ABANDONED")
        void shouldNotAbandonCompleted() {
            CatalogDraft draft = draftWithAllRequiredPhotos();
            draft.markCompleted();

            assertThrows(IllegalStateException.class, draft::abandon);
        }

        @Test
        @DisplayName("ABANDONED нельзя перевести в COMPLETED")
        void shouldNotCompleteAbandoned() {
            CatalogDraft draft = validDraft();
            draft.abandon();

            assertThrows(IllegalStateException.class, draft::markCompleted);
        }

        @Test
        @DisplayName("COMPLETED нельзя завершить повторно")
        void shouldNotCompleteAlreadyCompleted() {
            CatalogDraft draft = draftWithAllRequiredPhotos();
            draft.markCompleted();

            assertThrows(IllegalStateException.class, draft::markCompleted);
        }
    }

    // ──────────────────────────────────────────────
    // Вспомогательный метод
    // ──────────────────────────────────────────────
    private CatalogDraft draftWithAllRequiredPhotos() {
        CatalogDraft draft = validDraft();
        draft.addPhoto(PhotoType.BARCODE,     "drafts/123/barcode.jpg");
        draft.addPhoto(PhotoType.FRONT,       "drafts/123/front.jpg");
        draft.addPhoto(PhotoType.BACK,        "drafts/123/back.jpg");
        draft.addPhoto(PhotoType.INGREDIENTS, "drafts/123/ingredients.jpg");
        draft.addPhoto(PhotoType.NUTRITION,   "drafts/123/nutrition.jpg");
        draft.addPhoto(PhotoType.EXTRA,       "drafts/123/extra.jpg");
        return draft;
    }
}
