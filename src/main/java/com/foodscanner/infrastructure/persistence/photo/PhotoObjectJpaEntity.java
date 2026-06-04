package com.foodscanner.infrastructure.persistence.photo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(schema = "food_catalog", name = "photo_objects")
public class PhotoObjectJpaEntity {

    @Id
    @Column(length = 64)
    private String hash;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PhotoObjectJpaEntity() {}

    public PhotoObjectJpaEntity(String hash, String objectKey, Instant createdAt) {
        this.hash       = hash;
        this.objectKey  = objectKey;
        this.createdAt  = createdAt;
    }

    public String  getHash()      { return hash; }
    public String  getObjectKey() { return objectKey; }
    public Instant getCreatedAt() { return createdAt; }
}
