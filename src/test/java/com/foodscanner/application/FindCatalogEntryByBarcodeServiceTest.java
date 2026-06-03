package com.foodscanner.application;

import com.foodscanner.application.result.FindCatalogEntryResult;
import com.foodscanner.application.service.FindCatalogEntryByBarcodeService;
import com.foodscanner.domain.model.*;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FindCatalogEntryByBarcodeUseCase")
class FindCatalogEntryByBarcodeServiceTest {

    private CatalogEntryRepository         entryRepository;
    private FindCatalogEntryByBarcodeService service;

    @BeforeEach
    void setUp() {
        entryRepository = mock(CatalogEntryRepository.class);
        service         = new FindCatalogEntryByBarcodeService(entryRepository);
    }

    @Test
    @DisplayName("Возвращает entry если найдена по штрихкоду")
    void shouldReturnEntryWhenFound() {
        CatalogEntry entry = CatalogEntry.create(
            new Barcode("4607038310042"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of(
                PhotoType.BARCODE,     "b.jpg",
                PhotoType.FRONT,       "f.jpg",
                PhotoType.BACK,        "ba.jpg",
                PhotoType.INGREDIENTS, "i.jpg",
                PhotoType.NUTRITION,   "n.jpg",
                PhotoType.EXTRA,       "e.jpg"
            )
        );

        when(entryRepository.findByBarcode(new Barcode("4607038310042")))
            .thenReturn(Optional.of(entry));

        FindCatalogEntryResult result = service.execute("4607038310042");

        assertNotNull(result);
        assertEquals("4607038310042", result.getBarcode());
        assertEquals(6, result.getPhotos().size());
    }

    @Test
    @DisplayName("Возвращает null если не найдена")
    void shouldReturnNullWhenNotFound() {
        when(entryRepository.findByBarcode(any())).thenReturn(Optional.empty());

        assertNull(service.execute("0000000000000"));
    }
}
