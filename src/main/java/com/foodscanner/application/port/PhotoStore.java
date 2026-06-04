package com.foodscanner.application.port;

/**
 * Слой: application (порт)
 * Сохранение фото с дедупликацией по содержимому (Блок 16).
 */
public interface PhotoStore {
    /**
     * Сохраняет full + thumbnail с дедупликацией по SHA-256(full).
     * Если такой контент уже есть — переиспользует существующий объект.
     * Возвращает storageKey (object key), который пишется в БД.
     */
    String store(byte[] full, byte[] thumbnail, String contentType);
}
