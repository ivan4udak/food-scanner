package com.foodscanner.infrastructure.persistence.extraction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Слой: infrastructure (JPA). Задача структурного извлечения продукта. */
@Entity
@Table(schema = "food_catalog", name = "product_extraction_jobs")
public class ProductExtractionJobJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "ocr_job_id", nullable = false, columnDefinition = "uuid")
    private UUID ocrJobId;

    @Column(length = 64)
    private String barcode;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private short status;

    @Column(nullable = false)
    private int attempts;

    // ── результат (заполняет worker в след. срезах) ──
    @Column(length = 32)
    private String source;
    @Column(columnDefinition = "text")
    private String name;
    @Column(columnDefinition = "text")
    private String brand;
    @Column(columnDefinition = "text")
    private String manufacturer;
    @Column(columnDefinition = "text")
    private String composition;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String nutrition;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String confidence;
    @Column(name = "needs_review")
    private Boolean needsReview;
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductExtractionJobJpaEntity() {}

    public ProductExtractionJobJpaEntity(UUID id, UUID ocrJobId, String barcode, String type, short status,
                                         int attempts, Instant queuedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ocrJobId = ocrJobId;
        this.barcode = barcode;
        this.type = type;
        this.status = status;
        this.attempts = attempts;
        this.queuedAt = queuedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOcrJobId() { return ocrJobId; }
    public String getBarcode() { return barcode; }
    public String getType() { return type; }
    public short getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getQueuedAt() { return queuedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
