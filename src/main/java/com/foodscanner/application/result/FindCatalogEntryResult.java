package com.foodscanner.application.result;

import com.foodscanner.domain.model.CatalogEntry;
import com.foodscanner.domain.model.CatalogEntryPhoto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Слой: application
 * Результат FindCatalogEntryByBarcodeUseCase.
 * Проекция CatalogEntry — не сам domain объект наружу.
 */
public final class FindCatalogEntryResult {

    public record PhotoInfo(UUID id, String type, String storageKey) {}

    private final UUID           id;
    private final String         barcode;
    private final UUID           contributorId;
    private final List<PhotoInfo> photos;
    private final Instant        createdAt;

    public FindCatalogEntryResult(UUID id, String barcode, UUID contributorId,
                                  List<PhotoInfo> photos, Instant createdAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.photos        = photos;
        this.createdAt     = createdAt;
    }

    public static FindCatalogEntryResult from(CatalogEntry entry) {
        List<PhotoInfo> photos = entry.getPhotos().stream()
            .map(p -> new PhotoInfo(p.getId(), p.getType().name(), p.getStorageKey()))
            .collect(Collectors.toList());
        return new FindCatalogEntryResult(
            entry.getId(), entry.getBarcode().getValue(),
            entry.getContributorId(), photos, entry.getCreatedAt());
    }

    public UUID            getId()            { return id; }
    public String          getBarcode()       { return barcode; }
    public UUID            getContributorId() { return contributorId; }
    public List<PhotoInfo> getPhotos()        { return photos; }
    public Instant         getCreatedAt()     { return createdAt; }
}
