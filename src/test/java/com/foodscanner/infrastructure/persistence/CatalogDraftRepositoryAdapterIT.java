package com.foodscanner.infrastructure.persistence;

import com.foodscanner.domain.model.*;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CatalogDraftRepository — интеграционный тест")
class CatalogDraftRepositoryAdapterIT extends AbstractRepositoryIT {

    @Autowired ContributorRepository contributorRepository;
    @Autowired CatalogDraftRepository draftRepository;

    private UUID savedContributorId() {
        Contributor c = Contributor.create("tester_" + UUID.randomUUID().toString().substring(0, 6));
        contributorRepository.save(c);
        return c.getId();
    }

    @Test
    @DisplayName("Сохраняет черновик и находит по id")
    void shouldSaveAndFindById() {
        UUID contributorId = savedContributorId();
        CatalogDraft draft = CatalogDraft.create(
            new Barcode("1000000000001"), contributorId);
        draftRepository.save(draft);

        Optional<CatalogDraft> found = draftRepository.findById(draft.getId());
        assertTrue(found.isPresent());
        assertEquals("1000000000001", found.get().getBarcode().getValue());
        assertEquals(CatalogDraftStatus.OPEN, found.get().getStatus());
    }

    @Test
    @DisplayName("Сохраняет черновик с фотографиями")
    void shouldSaveDraftWithPhotos() {
        UUID contributorId = savedContributorId();
        CatalogDraft draft = CatalogDraft.create(
            new Barcode("1000000000002"), contributorId);
        draft.addPhoto(PhotoType.FRONT, "drafts/front.jpg");
        draft.addPhoto(PhotoType.BACK,  "drafts/back.jpg");
        draftRepository.save(draft);

        CatalogDraft found = draftRepository.findById(draft.getId()).orElseThrow();
        assertEquals(2, found.getPhotos().size());
    }

    @Test
    @DisplayName("findOpenByBarcodeAndContributor находит OPEN черновик")
    void shouldFindOpenDraft() {
        UUID contributorId = savedContributorId();
        CatalogDraft draft = CatalogDraft.create(
            new Barcode("1000000000003"), contributorId);
        draftRepository.save(draft);

        Optional<CatalogDraft> found = draftRepository.findOpenByBarcodeAndContributor(
            new Barcode("1000000000003"), contributorId);

        assertTrue(found.isPresent());
        assertEquals(draft.getId(), found.get().getId());
    }

    @Test
    @DisplayName("findOpenByBarcodeAndContributor не находит ABANDONED черновик")
    void shouldNotFindAbandonedDraft() {
        UUID contributorId = savedContributorId();
        CatalogDraft draft = CatalogDraft.create(
            new Barcode("1000000000004"), contributorId);
        draft.abandon();
        draftRepository.save(draft);

        Optional<CatalogDraft> found = draftRepository.findOpenByBarcodeAndContributor(
            new Barcode("1000000000004"), contributorId);

        assertFalse(found.isPresent());
    }
}
