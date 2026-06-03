package com.foodscanner.domain.model;

/**
 * Слой: domain
 *
 * Тип фотографии продукта.
 *
 * Все шесть типов обязательны для завершения каталога
 * (определено в CatalogCompletionPolicy.REQUIRED_TYPES).
 *
 * Расширение: при появлении OCR (Этап 2) политика будет запускать
 * распознавание именно по INGREDIENTS и NUTRITION фото.
 */
public enum PhotoType {
    BARCODE,
    FRONT,
    BACK,
    INGREDIENTS,
    NUTRITION,
    EXTRA
}
