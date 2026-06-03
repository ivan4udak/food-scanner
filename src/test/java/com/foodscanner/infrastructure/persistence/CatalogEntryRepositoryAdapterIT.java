package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.*;
import com.foodscanner.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CatalogEntryRepository — интеграционный тест")
class CatalogEntryRepositoryAdapterIT extends AbstractRepositoryIT {

    @Autowired ContributorRepository  contributorRepository;
    @Autowired CatalogDraftRepository draftRepository;
    @Autowired CatalogEntryRepository entryRepository;

    private CatalogEntry savedEntry(String barcode) {
        Contributor c = Contributor.create("c_" + UUID.randomUUID().toString().substring(0, 6));
        contributorRepository.save(c);

        CatalogDraft draft = CatalogDraft.create(new Barcode(barcode), c.getId());
        draftRepository.save(draft);

        CatalogEntry entry = CatalogEntry.create(
            new Barcode(barcode), c.getId(), draft.getId(),
            Map.of(
                PhotoType.BARCODE,     "b.jpg",
                PhotoType.FRONT,       "f.jpg",
                PhotoType.BACK,        "ba.jpg",
                PhotoType.INGREDIENTS, "i.jpg",
                PhotoType.NUTRITION,   "n.jpg",
                PhotoType.EXTRA,       "e.jpg"
            )
        );
        entryRepository.save(entry);
        return entry;
    }

    @Test
    @DisplayName("Сохраняет и находит CatalogEntry по штрихкоду")
    void shouldSaveAndFindByBarcode() {
        savedEntry("2000000000001");
        assertTrue(entryRepository.findByBarcode(new Barcode("2000000000001")).isPresent());
    }

    @Test
    @DisplayName("CatalogEntry содержит 6 фотографий после сохранения")
    void shouldPersistAllSixPhotos() {
        savedEntry("2000000000002");
        CatalogEntry found = entryRepository.findByBarcode(
            new Barcode("2000000000002")).orElseThrow();
        assertEquals(6, found.getPhotos().size());
    }

    @Test
    @DisplayName("existsByBarcode возвращает true для существующего")
    void shouldReturnTrueForExistingBarcode() {
        savedEntry("2000000000003");
        assertTrue(entryRepository.existsByBarcode(new Barcode("2000000000003")));
    }

    @Test
    @DisplayName("existsByBarcode возвращает false для несуществующего")
    void shouldReturnFalseForMissingBarcode() {
        assertFalse(entryRepository.existsByBarcode(new Barcode("9999999999999")));
    }
}
