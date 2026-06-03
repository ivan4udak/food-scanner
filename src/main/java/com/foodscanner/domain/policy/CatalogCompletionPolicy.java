package com.foodscanner.domain.policy;

import com.foodscanner.domain.exception.CatalogNotCompletableException;
import com.foodscanner.domain.model.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Слой: domain
 * Тип: Domain Service
 *
 * Зачем Domain Service: оперирует двумя агрегатами (CatalogDraft → CatalogEntry).
 * Агрегат не знает о другом агрегате — это ответственность Domain Service.
 * "Какие фото обязательны" — бизнес-правило, не оркестрация.
 *
 * Stateless: один экземпляр, переиспользовать.
 *
 * Расширение: при разных политиках по категориям продуктов —
 * выделить интерфейс CompletionPolicy и несколько реализаций.
 */
public final class CatalogCompletionPolicy {

    /**
     * Четыре обязательных типа: штрихкод, лицевая сторона, состав, пищевая ценность.
     * BACK и EXTRA — опциональны: можно загрузить дополнительно, но они
     * не блокируют завершение каталога.
     */
    public static final Set<PhotoType> REQUIRED_TYPES = EnumSet.of(
        PhotoType.BARCODE,
        PhotoType.FRONT,
        PhotoType.INGREDIENTS,
        PhotoType.NUTRITION
    );

    public boolean canComplete(CatalogDraft draft) {
        return findMissing(draft).isEmpty();
    }

    /**
     * Возвращает недостающие обязательные типы.
     * Используется для прогресс-бара в UI (0/4 → 4/4).
     */
    public Set<PhotoType> findMissing(CatalogDraft draft) {
        Set<PhotoType> present = draft.getPhotos().stream()
            .map(DraftPhoto::getType)
            .collect(Collectors.toSet());

        return REQUIRED_TYPES.stream()
            .filter(t -> !present.contains(t))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(PhotoType.class)));
    }

    /**
     * Создаёт CatalogEntry из черновика.
     *
     * Сохраняются ВСЕ загруженные фото — и обязательные, и опциональные
     * (BACK, EXTRA). При нескольких фото одного типа берётся последнее.
     * capturedAt переносится у выигравшего фото каждого типа.
     * Переводит черновик в COMPLETED.
     *
     * @throws CatalogNotCompletableException если не хватает обязательных фото
     */
    public CatalogEntry createEntry(CatalogDraft draft) {
        Set<PhotoType> missing = findMissing(draft);
        if (!missing.isEmpty()) {
            throw new CatalogNotCompletableException(missing);
        }

        // HashMap/EnumMap, а не Collectors.toMap — capturedAt может быть null.
        Map<PhotoType, String>  storageKeyByType = new java.util.EnumMap<>(PhotoType.class);
        Map<PhotoType, Instant> capturedAtByType = new java.util.EnumMap<>(PhotoType.class);
        for (DraftPhoto p : draft.getPhotos()) {
            storageKeyByType.put(p.getType(), p.getStorageKey());
            capturedAtByType.put(p.getType(), p.getCapturedAt());
        }

        CatalogEntry entry = CatalogEntry.create(
            draft.getBarcode(),
            draft.getContributorId(),
            draft.getId(),
            storageKeyByType,
            capturedAtByType
        );

        draft.markCompleted();

        return entry;
    }
}
