package com.foodscanner.domain.repository;

import java.util.Optional;

/**
 * Слой: domain
 * Реестр контент-адресных объектов (дедупликация по SHA-256, Блок 16).
 */
public interface PhotoObjectRepository {
    /** Ключ объекта по хэшу содержимого, если такой контент уже загружен. */
    Optional<String> findObjectKeyByHash(String hash);

    /** Регистрирует соответствие hash → objectKey. */
    void register(String hash, String objectKey);

    /** Есть ли ещё ссылки на объект (в draft_photos или catalog_entry_photos). */
    boolean isObjectKeyReferenced(String objectKey);

    /** Удаляет запись реестра по ключу объекта. */
    void deleteByObjectKey(String objectKey);
}
