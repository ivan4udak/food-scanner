package com.foodscanner.infrastructure.persistence.ocr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Слой: infrastructure (JPA). OCR-задача. */
@Entity
@Table(schema = "food_catalog", name = "ocr_jobs")
public class OcrJobJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "draft_id", columnDefinition = "uuid")
    private UUID draftId;

    @Column(name = "catalog_entry_id", columnDefinition = "uuid")
    private UUID catalogEntryId;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "photo_type", nullable = false, length = 20)
    private String photoType;

    @Column(nullable = false)
    private short status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "parsed_ingredients", columnDefinition = "text")
    private String parsedIngredients;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_nutrition", columnDefinition = "jsonb")
    private String parsedNutrition;

    @Column
    private Double confidence;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OcrJobJpaEntity() {}

    public OcrJobJpaEntity(UUID id, UUID draftId, UUID catalogEntryId, String storageKey, String photoType,
                           short status, int attempts, String rawText, String parsedIngredients,
                           String parsedNutrition, Double confidence, String errorCode, String errorMessage,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.draftId = draftId;
        this.catalogEntryId = catalogEntryId;
        this.storageKey = storageKey;
        this.photoType = photoType;
        this.status = status;
        this.attempts = attempts;
        this.rawText = rawText;
        this.parsedIngredients = parsedIngredients;
        this.parsedNutrition = parsedNutrition;
        this.confidence = confidence;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getDraftId() { return draftId; }
    public UUID getCatalogEntryId() { return catalogEntryId; }
    public String getStorageKey() { return storageKey; }
    public String getPhotoType() { return photoType; }
    public short getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getRawText() { return rawText; }
    public String getParsedIngredients() { return parsedIngredients; }
    public String getParsedNutrition() { return parsedNutrition; }
    public Double getConfidence() { return confidence; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
