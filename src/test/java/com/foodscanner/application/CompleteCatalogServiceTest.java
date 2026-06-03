package com.foodscanner.application;

import com.foodscanner.application.command.CompleteCatalogCommand;
import com.foodscanner.application.result.CompleteCatalogResult;
import com.foodscanner.application.service.CompleteCatalogService;
import com.foodscanner.domain.exception.CatalogDraftNotFoundException;
import com.foodscanner.domain.exception.CatalogNotCompletableException;
import com.foodscanner.domain.model.*;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.CatalogEntryRepository;
import com.foodscanner.domain.repository.ContributorRepository;
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
 * Ключевой транзакционный use case:
 *   1. Найти черновик
 *   2. Проверить владельца
 *   3. Создать CatalogEntry через policy
 *   4. Сохранить CatalogEntry
 *   5. Перевести черновик в COMPLETED + сохранить
 *   6. Инкрементировать Contributor.completedCatalogCount + сохранить
 *
 * Все шесть шагов атомарны (@Transactional в сервисе).
 */
@DisplayName("CompleteCatalogUseCase")
class CompleteCatalogServiceTest {

    private CatalogDraftRepository  draftRepository;
    private CatalogEntryRepository  entryRepository;
    private ContributorRepository   contributorRepository;
    private CatalogCompletionPolicy policy;
    private CompleteCatalogService  service;

    private static final UUID CONTRIBUTOR_ID = UUID.randomUUID();
    private static final UUID OTHER_ID       = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        draftRepository       = mock(CatalogDraftRepository.class);
        entryRepository       = mock(CatalogEntryRepository.class);
        contributorRepository = mock(ContributorRepository.class);
        policy                = new CatalogCompletionPolicy();
        service               = new CompleteCatalogService(
            draftRepository, entryRepository, contributorRepository, policy);
    }

    private CatalogDraft completeDraft() {
        CatalogDraft draft = CatalogDraft.create(
            new Barcode("4607038310042"), CONTRIBUTOR_ID);
        draft.addPhoto(PhotoType.BARCODE,     "b.jpg");
        draft.addPhoto(PhotoType.FRONT,       "f.jpg");
        draft.addPhoto(PhotoType.BACK,        "ba.jpg");
        draft.addPhoto(PhotoType.INGREDIENTS, "i.jpg");
        draft.addPhoto(PhotoType.NUTRITION,   "n.jpg");
        draft.addPhoto(PhotoType.EXTRA,       "e.jpg");
        return draft;
    }

    private Contributor contributor() {
        return Contributor.create("alice");
    }

    @Nested
    @DisplayName("Успешное завершение каталога")
    class Success {

        @Test
        @DisplayName("Создаёт CatalogEntry и возвращает её id")
        void shouldCreateCatalogEntryAndReturnId() {
            CatalogDraft  draft = completeDraft();
            Contributor   c     = contributor();

            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(contributorRepository.findById(CONTRIBUTOR_ID)).thenReturn(Optional.of(c));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(contributorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompleteCatalogResult result = service.execute(
                new CompleteCatalogCommand(draft.getId(), CONTRIBUTOR_ID));

            assertNotNull(result.getCatalogEntryId());
        }

        @Test
        @DisplayName("Инкрементирует completedCatalogCount контрибьютора")
        void shouldIncrementContributorCount() {
            CatalogDraft draft = completeDraft();
            Contributor  c     = contributor();
            assertEquals(0, c.getCompletedCatalogCount());

            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(contributorRepository.findById(CONTRIBUTOR_ID)).thenReturn(Optional.of(c));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(contributorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompleteCatalogResult result = service.execute(
                new CompleteCatalogCommand(draft.getId(), CONTRIBUTOR_ID));

            assertEquals(1, result.getContributorCompletedCount());
            assertEquals(1, c.getCompletedCatalogCount());
        }

        @Test
        @DisplayName("Переводит черновик в статус COMPLETED")
        void shouldMarkDraftCompleted() {
            CatalogDraft draft = completeDraft();
            Contributor  c     = contributor();

            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(contributorRepository.findById(CONTRIBUTOR_ID)).thenReturn(Optional.of(c));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(contributorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new CompleteCatalogCommand(draft.getId(), CONTRIBUTOR_ID));

            assertEquals(CatalogDraftStatus.COMPLETED, draft.getStatus());
        }

        @Test
        @DisplayName("Сохраняет все три агрегата: entry, draft, contributor")
        void shouldSaveAllThreeAggregates() {
            CatalogDraft draft = completeDraft();
            Contributor  c     = contributor();

            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(contributorRepository.findById(CONTRIBUTOR_ID)).thenReturn(Optional.of(c));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(contributorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new CompleteCatalogCommand(draft.getId(), CONTRIBUTOR_ID));

            verify(entryRepository).save(any(CatalogEntry.class));
            verify(draftRepository).save(draft);
            verify(contributorRepository).save(c);
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
                () -> service.execute(
                    new CompleteCatalogCommand(unknownId, CONTRIBUTOR_ID)));
        }

        @Test
        @DisplayName("Бросает исключение если черновик принадлежит другому контрибьютору")
        void shouldThrowWhenDraftBelongsToOther() {
            CatalogDraft draft = completeDraft(); // создан с CONTRIBUTOR_ID
            when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThrows(IllegalStateException.class,
                () -> service.execute(
                    new CompleteCatalogCommand(draft.getId(), OTHER_ID)));
        }

        @Test
        @DisplayName("Бросает исключение если не все фото загружены")
        void shouldThrowWhenDraftNotComplete() {
            CatalogDraft incompleteDraft = CatalogDraft.create(
                new Barcode("4607038310042"), CONTRIBUTOR_ID);
            incompleteDraft.addPhoto(PhotoType.FRONT, "f.jpg");

            Contributor c = contributor();

            when(draftRepository.findById(incompleteDraft.getId()))
                .thenReturn(Optional.of(incompleteDraft));
            when(contributorRepository.findById(CONTRIBUTOR_ID))
                .thenReturn(Optional.of(c));

            assertThrows(CatalogNotCompletableException.class,
                () -> service.execute(
                    new CompleteCatalogCommand(incompleteDraft.getId(), CONTRIBUTOR_ID)));
        }

        @Test
        @DisplayName("Не сохраняет ничего если черновик неполный")
        void shouldNotSaveAnythingWhenIncomplete() {
            CatalogDraft incompleteDraft = CatalogDraft.create(
                new Barcode("4607038310042"), CONTRIBUTOR_ID);
            Contributor c = contributor();

            when(draftRepository.findById(incompleteDraft.getId()))
                .thenReturn(Optional.of(incompleteDraft));
            when(contributorRepository.findById(CONTRIBUTOR_ID))
                .thenReturn(Optional.of(c));

            try {
                service.execute(
                    new CompleteCatalogCommand(incompleteDraft.getId(), CONTRIBUTOR_ID));
            } catch (CatalogNotCompletableException ignored) {}

            verify(entryRepository, never()).save(any());
            verify(contributorRepository, never()).save(any());
        }
    }
}
