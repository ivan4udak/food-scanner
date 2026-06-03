package com.foodscanner.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Aggregate Root
 *
 * Зачем: черновик процесса каталогизации. Существует пока контрибьютор
 * фотографирует продукт. Не засоряет каталог незавершёнными данными —
 * CatalogEntry создаётся только из завершённого черновика.
 *
 * Зависимости: только domain.model (нет Spring, нет JPA).
 *
 * Инварианты:
 *   1. barcode не null
 *   2. contributorId не null
 *   3. status при создании — OPEN
 *   4. нельзя добавить фото в COMPLETED или ABANDONED
 *   5. COMPLETED и ABANDONED — терминальные
 *   6. ABANDONED нельзя перевести в COMPLETED
 *   7. getPhotos() защищена от внешней мутации
 *
 * Расширение: добавить доменные события (DomainEvent) на markCompleted()
 * при появлении асинхронной реакции (например, уведомление в Этапе 4).
 */
public final class CatalogDraft {

    private final UUID               id;
    private final Barcode            barcode;
    private final UUID               contributorId;
    private       CatalogDraftStatus status;
    private final List<DraftPhoto>   photos;
    private final Instant            createdAt;
    private       Instant            updatedAt;

    private CatalogDraft(
            UUID id,
            Barcode barcode,
            UUID contributorId,
            CatalogDraftStatus status,
            List<DraftPhoto> photos,
            Instant createdAt,
            Instant updatedAt) {
        this.id            = id;
        this.barcode       = barcode;
        this.contributorId = contributorId;
        this.status        = status;
        this.photos        = new ArrayList<>(photos);
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    // ──────────────────────────────────────────────
    // Фабричный метод
    // ──────────────────────────────────────────────

    public static CatalogDraft create(Barcode barcode, UUID contributorId) {
        if (barcode == null) {
            throw new IllegalArgumentException("Barcode must not be null");
        }
        if (contributorId == null) {
            throw new IllegalArgumentException("ContributorId must not be null");
        }
        Instant now = Instant.now();
        return new CatalogDraft(
            UUID.randomUUID(), barcode, contributorId,
            CatalogDraftStatus.OPEN, Collections.emptyList(), now, now);
    }

    /** Восстановление из хранилища. Используется только в CatalogDraftRepositoryAdapter. */
    public static CatalogDraft reconstitute(
            UUID id, Barcode barcode, UUID contributorId,
            CatalogDraftStatus status, List<DraftPhoto> photos,
            Instant createdAt, Instant updatedAt) {
        Objects.requireNonNull(id,            "id must not be null");
        Objects.requireNonNull(barcode,       "barcode must not be null");
        Objects.requireNonNull(contributorId, "contributorId must not be null");
        Objects.requireNonNull(status,        "status must not be null");
        Objects.requireNonNull(photos,        "photos must not be null");
        Objects.requireNonNull(createdAt,     "createdAt must not be null");
        Objects.requireNonNull(updatedAt,     "updatedAt must not be null");
        return new CatalogDraft(id, barcode, contributorId, status, photos, createdAt, updatedAt);
    }

    // ──────────────────────────────────────────────
    // Управление фотографиями
    // ──────────────────────────────────────────────

    /**
     * Добавляет фото к черновику.
     * Разрешено только в статусе OPEN.
     * Несколько фото одного типа допустимы — контрибьютор мог перефотографировать.
     */
    public void addPhoto(PhotoType type, String storageKey) {
        addPhoto(type, storageKey, null);
    }

    /**
     * Добавляет фото с датой съёмки (capturedAt из метаданных галереи).
     * capturedAt может быть null (камера / нет EXIF).
     */
    public void addPhoto(PhotoType type, String storageKey, Instant capturedAt) {
        if (type == null) {
            throw new IllegalArgumentException("PhotoType must not be null");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("StorageKey must not be null or blank");
        }
        if (status != CatalogDraftStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot add photo to draft with status: " + status);
        }
        photos.add(new DraftPhoto(this.id, type, storageKey, capturedAt));
        this.updatedAt = Instant.now();
    }

    // ──────────────────────────────────────────────
    // Переходы статусов
    // ──────────────────────────────────────────────

    /**
     * Переводит черновик в COMPLETED.
     * Вызывается только из CatalogCompletionPolicy после проверки полноты.
     * Package-private — внешний код не может вызвать напрямую.
     */
    public void markCompleted() {
        if (status != CatalogDraftStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot complete draft with status: " + status);
        }
        this.status    = CatalogDraftStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    /**
     * Переводит черновик в ABANDONED.
     * Допустимо только из OPEN.
     */
    public void abandon() {
        if (status != CatalogDraftStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot abandon draft with status: " + status);
        }
        this.status    = CatalogDraftStatus.ABANDONED;
        this.updatedAt = Instant.now();
    }

    // ──────────────────────────────────────────────
    // Геттеры
    // ──────────────────────────────────────────────

    public UUID               getId()            { return id; }
    public Barcode            getBarcode()       { return barcode; }
    public UUID               getContributorId() { return contributorId; }
    public CatalogDraftStatus getStatus()        { return status; }
    public Instant            getCreatedAt()     { return createdAt; }
    public Instant            getUpdatedAt()     { return updatedAt; }

    /** Возвращает unmodifiable список. Мутация только через addPhoto(). */
    public List<DraftPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogDraft other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "CatalogDraft{id=" + id + ", barcode=" + barcode + ", status=" + status + "}";
    }
}
