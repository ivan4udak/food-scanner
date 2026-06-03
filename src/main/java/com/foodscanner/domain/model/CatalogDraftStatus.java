package com.foodscanner.domain.model;

/**
 * Слой: domain
 *
 * Жизненный цикл черновика каталога.
 *
 * Переходы:
 *   OPEN → COMPLETED   (markCompleted, только через CatalogCompletionPolicy)
 *   OPEN → ABANDONED   (abandon)
 *
 * COMPLETED и ABANDONED — терминальные статусы.
 */
public enum CatalogDraftStatus {
    OPEN,
    COMPLETED,
    ABANDONED
}
