package com.foodscanner.application.port;

/**
 * Слой: application (порт)
 *
 * Абстракция объектного хранилища фотографий. Доменный/прикладной код
 * работает только с этим интерфейсом — реализация (MinIO, S3, …) живёт
 * в infrastructure и подключается через Spring.
 *
 * Расширение: добавить S3PhotoStorage рядом с MinioPhotoStorage,
 * выбор через свойство storage.provider — use case не меняется.
 */
public interface PhotoStorage {

    /**
     * Заливает байты в хранилище под заданным ключом и возвращает его же
     * (storageKey, который сохраняется в БД). Исходный формат не меняется.
     */
    String upload(byte[] content, String contentType, String objectKey);

    /** Скачивает объект по ключу. */
    StoredObject download(String objectKey);

    /** Удаляет объект по ключу (идемпотентно: отсутствующий объект — не ошибка). */
    void delete(String objectKey);

    /** Содержимое + content-type для отдачи клиенту. */
    record StoredObject(byte[] content, String contentType) {}
}
