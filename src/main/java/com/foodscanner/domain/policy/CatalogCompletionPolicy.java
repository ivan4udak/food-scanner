package com.foodscanner.domain.policy;

import com.foodscanner.domain.exception.CatalogNotCompletableException;
import com.foodscanner.domain.model.*;

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
     * Все шесть типов обязательны.
     * EXTRA — обязательный дополнительный снимок (упаковка сбоку, дно и т.п.).
     */
    public static final Set<PhotoType> REQUIRED_TYPES = EnumSet.of(
        PhotoType.BARCODE,
        PhotoType.FRONT,
        PhotoType.BACK,
        PhotoType.INGREDIENTS,
        PhotoType.NUTRITION,
        PhotoType.EXTRA
    );

    public boolean canComplete(CatalogDraft draft) {
        return findMissing(draft).isEmpty();
    }

    /**
     * Возвращает недостающие обязательные типы.
     * Используется для прогресс-бара в UI (0/6 → 6/6).
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
     * При нескольких фото одного типа берётся последнее добавленное.
     * Переводит черновик в COMPLETED.
     *
     * @throws CatalogNotCompletableException если не хватает обязательных фото
     */
    public CatalogEntry createEntry(CatalogDraft draft) {
        Set<PhotoType> missing = findMissing(draft);
        if (!missing.isEmpty()) {
            throw new CatalogNotCompletableException(missing);
        }

        Map<PhotoType, String> storageKeyByType = draft.getPhotos().stream()
            .filter(p -> REQUIRED_TYPES.contains(p.getType()))
            .collect(Collectors.toMap(
                DraftPhoto::getType,
                DraftPhoto::getStorageKey,
                (existing, replacement) -> replacement
            ));

        CatalogEntry entry = CatalogEntry.create(
            draft.getBarcode(),
            draft.getContributorId(),
            draft.getId(),
            storageKeyByType
        );

        draft.markCompleted();

        return entry;
    }
}
