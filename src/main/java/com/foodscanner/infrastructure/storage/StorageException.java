package com.foodscanner.infrastructure.storage;

/** Ошибка работы с объектным хранилищем. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
