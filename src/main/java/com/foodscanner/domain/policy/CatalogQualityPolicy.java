package com.foodscanner.domain.policy;

import java.util.Set;

/**
 * Слой: domain (политика).
 * Оценка качества записи каталога БЕЗ OCR — по набору присутствующих типов фото.
 * Веса (макс. 100): BARCODE 20, FRONT 20, INGREDIENTS 25, NUTRITION 25, BACK|EXTRA 10.
 */
public final class CatalogQualityPolicy {

    private CatalogQualityPolicy() {}

    /** @param presentTypes имена типов фото (BARCODE/FRONT/...). @return 0..100. */
    public static int score(Set<String> presentTypes) {
        if (presentTypes == null || presentTypes.isEmpty()) return 0;
        int s = 0;
        if (presentTypes.contains("BARCODE")) s += 20;
        if (presentTypes.contains("FRONT")) s += 20;
        if (presentTypes.contains("INGREDIENTS")) s += 25;
        if (presentTypes.contains("NUTRITION")) s += 25;
        if (presentTypes.contains("BACK") || presentTypes.contains("EXTRA")) s += 10;
        return Math.min(100, s);
    }
}
