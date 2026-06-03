package com.foodscanner.infrastructure.storage;

import com.foodscanner.application.port.PhotoStorage;
import io.minio.*;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;

/**
 * Слой: infrastructure
 * Реализация PhotoStorage поверх MinIO (S3-совместимое API).
 *
 * Исходный формат фото сохраняется как есть — кладём байты «как пришли»
 * с указанным content-type, без перекодирования.
 */
public class MinioPhotoStorage implements PhotoStorage {

    private final MinioClient client;
    private final String      bucket;

    public MinioPhotoStorage(MinioClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    /** Создаёт бакет при старте, если его ещё нет. */
    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new StorageException("Failed to ensure bucket '" + bucket + "'", e);
        }
    }

    @Override
    public String upload(byte[] content, String contentType, String objectKey) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(in, content.length, -1)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build());
            return objectKey;
        } catch (Exception e) {
            throw new StorageException("Failed to upload object '" + objectKey + "'", e);
        }
    }

    @Override
    public StoredObject download(String objectKey) {
        try {
            StatObjectResponse stat = client.statObject(
                StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            try (GetObjectResponse resp = client.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
                return new StoredObject(resp.readAllBytes(), stat.contentType());
            }
        } catch (Exception e) {
            throw new StorageException("Failed to download object '" + objectKey + "'", e);
        }
    }
}
