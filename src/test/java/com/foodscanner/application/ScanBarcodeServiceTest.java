package com.foodscanner.application;

import com.foodscanner.application.command.ScanBarcodeCommand;
import com.foodscanner.application.result.ScanBarcodeResult;
import com.foodscanner.application.result.ScanBarcodeResult.ScanStatus;
import com.foodscanner.application.service.ScanBarcodeService;
import com.foodscanner.domain.model.Barcode;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.CatalogDraftStatus;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.CatalogEntryRepository;
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
 * Бизнес-сценарии ScanBarcodeUseCase:
 *   1. CatalogEntry существует → EXISTS, черновик не создаётся
 *   2. OPEN черновик у этого контрибьютора уже есть → NEW + вернуть существующий draftId
 *   3. Ничего нет → NEW + создать новый черновик
 */
@DisplayName("ScanBarcodeUseCase")
class ScanBarcodeServiceTest {

    private CatalogEntryRepository entryRepository;
    private CatalogDraftRepository draftRepository;
    private ScanBarcodeService     service;

    private static final UUID CONTRIBUTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        entryRepository = mock(CatalogEntryRepository.class);
        draftRepository = mock(CatalogDraftRepository.class);
        service         = new ScanBarcodeService(entryRepository, draftRepository);
    }

    @Nested
    @DisplayName("Штрихкод уже каталогизирован")
    class AlreadyExists {

        @Test
        @DisplayName("Возвращает статус EXISTS")
        void shouldReturnExistsStatus() {
            when(entryRepository.existsByBarcode(new Barcode("4607038310042")))
                .thenReturn(true);

            ScanBarcodeResult result = service.execute(
                new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            assertEquals(ScanStatus.EXISTS, result.getStatus());
        }

        @Test
        @DisplayName("draftId равен null при EXISTS")
        void shouldReturnNullDraftIdWhenExists() {
            when(entryRepository.existsByBarcode(any())).thenReturn(true);

            ScanBarcodeResult result = service.execute(
                new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            assertNull(result.getDraftId());
        }

        @Test
        @DisplayName("Черновик не создаётся и не сохраняется")
        void shouldNotCreateDraftWhenExists() {
            when(entryRepository.existsByBarcode(any())).thenReturn(true);

            service.execute(new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            verify(draftRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Штрихкод новый — черновик не существует")
    class NewBarcode {

        @Test
        @DisplayName("Возвращает статус NEW")
        void shouldReturnNewStatus() {
            when(entryRepository.existsByBarcode(any())).thenReturn(false);
            when(draftRepository.findOpenByBarcodeAndContributor(any(), any()))
                .thenReturn(Optional.empty());
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ScanBarcodeResult result = service.execute(
                new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            assertEquals(ScanStatus.NEW, result.getStatus());
        }

        @Test
        @DisplayName("Создаёт новый черновик и возвращает его draftId")
        void shouldCreateNewDraft() {
            when(entryRepository.existsByBarcode(any())).thenReturn(false);
            when(draftRepository.findOpenByBarcodeAndContributor(any(), any()))
                .thenReturn(Optional.empty());
            when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ScanBarcodeResult result = service.execute(
                new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            assertNotNull(result.getDraftId());
            verify(draftRepository).save(any(CatalogDraft.class));
        }

        @Test
        @DisplayName("Созданный черновик имеет статус OPEN")
        void shouldCreateOpenDraft() {
            when(entryRepository.existsByBarcode(any())).thenReturn(false);
            when(draftRepository.findOpenByBarcodeAndContributor(any(), any()))
                .thenReturn(Optional.empty());

            // Захватываем сохраняемый черновик
            when(draftRepository.save(any())).thenAnswer(inv -> {
                CatalogDraft draft = inv.getArgument(0);
                assertEquals(CatalogDraftStatus.OPEN, draft.getStatus());
                return draft;
            });

            service.execute(new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            verify(draftRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Штрихкод новый — OPEN черновик уже есть у этого контрибьютора")
    class ExistingOpenDraft {

        @Test
        @DisplayName("Возвращает статус NEW с существующим draftId")
        void shouldReturnExistingDraftId() {
            CatalogDraft existing = CatalogDraft.create(
                new Barcode("4607038310042"), CONTRIBUTOR_ID);

            when(entryRepository.existsByBarcode(any())).thenReturn(false);
            when(draftRepository.findOpenByBarcodeAndContributor(any(), eq(CONTRIBUTOR_ID)))
                .thenReturn(Optional.of(existing));

            ScanBarcodeResult result = service.execute(
                new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            assertEquals(ScanStatus.NEW, result.getStatus());
            assertEquals(existing.getId(), result.getDraftId());
        }

        @Test
        @DisplayName("Новый черновик не создаётся — возвращается существующий")
        void shouldNotCreateNewDraftWhenOpenExists() {
            CatalogDraft existing = CatalogDraft.create(
                new Barcode("4607038310042"), CONTRIBUTOR_ID);

            when(entryRepository.existsByBarcode(any())).thenReturn(false);
            when(draftRepository.findOpenByBarcodeAndContributor(any(), any()))
                .thenReturn(Optional.of(existing));

            service.execute(new ScanBarcodeCommand("4607038310042", CONTRIBUTOR_ID));

            verify(draftRepository, never()).save(any());
        }
    }
}
