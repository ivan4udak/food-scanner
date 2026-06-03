package com.foodscanner.domain.policy;

import com.foodscanner.domain.exception.CatalogNotCompletableException;

import com.foodscanner.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Слой: domain
 *
 * Тестирует Domain Service CatalogCompletionPolicy:
 * - canComplete: все комбинации полноты фото
 * - findMissing: точный список недостающих типов
 * - createEntry: создание CatalogEntry из завершённого черновика
 * - защита от создания из незавершённого черновика
 * - при нескольких фото одного типа берётся последнее
 */
@DisplayName("CatalogCompletionPolicy Domain Service")
class CatalogCompletionPolicyTest {

    private CatalogCompletionPolicy policy;
    private static final UUID CONTRIBUTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policy = new CatalogCompletionPolicy();
    }

    private CatalogDraft emptyDraft() {
        return CatalogDraft.create(new Barcode("4607038310042"), CONTRIBUTOR_ID);
    }

    private CatalogDraft draftWithAllRequired() {
        CatalogDraft draft = emptyDraft();
        draft.addPhoto(PhotoType.BARCODE,     "drafts/123/barcode.jpg");
        draft.addPhoto(PhotoType.FRONT,       "drafts/123/front.jpg");
        draft.addPhoto(PhotoType.BACK,        "drafts/123/back.jpg");
        draft.addPhoto(PhotoType.INGREDIENTS, "drafts/123/ingredients.jpg");
        draft.addPhoto(PhotoType.NUTRITION,   "drafts/123/nutrition.jpg");
        draft.addPhoto(PhotoType.EXTRA,       "drafts/123/extra.jpg");
        return draft;
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("canComplete")
    class CanComplete {

        @Test
        @DisplayName("Возвращает true при наличии всех пяти обязательных типов")
        void shouldReturnTrueWhenAllRequiredPresent() {
            assertTrue(policy.canComplete(draftWithAllRequired()));
        }

        @Test
        @DisplayName("Возвращает false для пустого черновика")
        void shouldReturnFalseForEmptyDraft() {
            assertFalse(policy.canComplete(emptyDraft()));
        }

        @Test
        @DisplayName("Возвращает false при отсутствии NUTRITION")
        void shouldReturnFalseWhenNutritionMissing() {
            CatalogDraft draft = emptyDraft();
            draft.addPhoto(PhotoType.BARCODE,     "b.jpg");
            draft.addPhoto(PhotoType.FRONT,       "f.jpg");
            draft.addPhoto(PhotoType.BACK,        "ba.jpg");
            draft.addPhoto(PhotoType.INGREDIENTS, "i.jpg");
            // NUTRITION намеренно не добавляем

            assertFalse(policy.canComplete(draft));
        }


        @Test
        @DisplayName("Возвращает true при нескольких фото одного типа")
        void shouldReturnTrueWithMultiplePhotosOfSameType() {
            CatalogDraft draft = draftWithAllRequired();
            draft.addPhoto(PhotoType.FRONT, "front_v2.jpg"); // второе FRONT

            assertTrue(policy.canComplete(draft));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("findMissing")
    class FindMissing {

        @Test
        @DisplayName("Возвращает четыре обязательных типа для пустого черновика")
        void shouldReturnAllRequiredForEmptyDraft() {
            Set<PhotoType> missing = policy.findMissing(emptyDraft());

            assertEquals(Set.of(
                PhotoType.BARCODE,
                PhotoType.FRONT,
                PhotoType.INGREDIENTS,
                PhotoType.NUTRITION
            ), missing);
        }

        @Test
        @DisplayName("Возвращает пустой набор когда всё загружено")
        void shouldReturnEmptyWhenComplete() {
            assertTrue(policy.findMissing(draftWithAllRequired()).isEmpty());
        }

        @Test
        @DisplayName("Возвращает только недостающие типы")
        void shouldReturnOnlyMissingTypes() {
            CatalogDraft draft = emptyDraft();
            draft.addPhoto(PhotoType.BARCODE, "b.jpg");
            draft.addPhoto(PhotoType.FRONT,   "f.jpg");

            Set<PhotoType> missing = policy.findMissing(draft);

            assertEquals(Set.of(
                PhotoType.INGREDIENTS,
                PhotoType.NUTRITION
            ), missing);
        }

    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createEntry")
    class CreateEntry {

        @Test
        @DisplayName("Создаёт CatalogEntry из черновика со всеми фото")
        void shouldCreateEntryFromCompleteDraft() {
            CatalogDraft draft = draftWithAllRequired();

            CatalogEntry entry = policy.createEntry(draft);

            assertNotNull(entry.getId());
            assertEquals("4607038310042", entry.getBarcode().getValue());
            assertEquals(CONTRIBUTOR_ID, entry.getContributorId());
            assertEquals(draft.getId(), entry.getDraftId());
        }

        @Test
        @DisplayName("CatalogEntry содержит ровно шесть фото")
        void shouldCreateEntryWithFivePhotos() {
            CatalogEntry entry = policy.createEntry(draftWithAllRequired());

            assertEquals(6, entry.getPhotos().size());
        }

        @Test
        @DisplayName("При нескольких фото одного типа берётся последнее")
        void shouldTakeLastPhotoOfEachType() {
            CatalogDraft draft = draftWithAllRequired();
            draft.addPhoto(PhotoType.FRONT, "front_final.jpg"); // заменяет первое FRONT

            CatalogEntry entry = policy.createEntry(draft);

            String frontKey = entry.getPhotos().stream()
                .filter(p -> p.getType() == PhotoType.FRONT)
                .findFirst()
                .map(CatalogEntryPhoto::getStorageKey)
                .orElseThrow();

            assertEquals("front_final.jpg", frontKey);
        }

        @Test
        @DisplayName("CatalogEntry содержит все обязательные типы фото")
        void shouldContainAllRequiredPhotoTypes() {
            CatalogEntry entry = policy.createEntry(draftWithAllRequired());

            Set<PhotoType> types = new java.util.HashSet<>();
            entry.getPhotos().forEach(p -> types.add(p.getType()));

            assertTrue(types.containsAll(Set.of(
                PhotoType.BARCODE,
                PhotoType.FRONT,
                PhotoType.BACK,
                PhotoType.INGREDIENTS,
                PhotoType.NUTRITION
            )));
        }

        @Test
        @DisplayName("Бросает исключение если черновик неполный")
        void shouldThrowWhenDraftIsNotComplete() {
            CatalogDraft draft = emptyDraft();
            draft.addPhoto(PhotoType.FRONT, "front.jpg");

            assertThrows(CatalogNotCompletableException.class,
                () -> policy.createEntry(draft));
        }

    }
}
