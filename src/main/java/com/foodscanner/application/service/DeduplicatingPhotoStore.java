package com.foodscanner.application.service;

import com.foodscanner.application.port.PhotoStorage;
import com.foodscanner.application.port.PhotoStore;
import com.foodscanner.domain.repository.PhotoObjectRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Слой: application
 * Контент-адресное хранение с дедупликацией: ключ объекта = photos/{sha256}.jpg.
 * Если контент уже загружен — повторно в MinIO не пишем, переиспользуем ключ.
 */
public class DeduplicatingPhotoStore implements PhotoStore {

    private final PhotoStorage          storage;
    private final PhotoObjectRepository registry;

    public DeduplicatingPhotoStore(PhotoStorage storage, PhotoObjectRepository registry) {
        this.storage  = storage;
        this.registry = registry;
    }

    @Override
    public String store(byte[] full, byte[] thumbnail, String contentType) {
        String hash = sha256(full);

        Optional<String> existing = registry.findObjectKeyByHash(hash);
        if (existing.isPresent()) {
            return existing.get();   // дубликат — переиспользуем
        }

        String objectKey = "photos/" + hash + ".jpg";
        storage.upload(full, contentType, objectKey);
        storage.upload(thumbnail, contentType, thumbKey(objectKey));
        registry.register(hash, objectKey);
        return objectKey;
    }

    public static String thumbKey(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? key + "_thumb" : key.substring(0, dot) + "_thumb" + key.substring(dot);
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
