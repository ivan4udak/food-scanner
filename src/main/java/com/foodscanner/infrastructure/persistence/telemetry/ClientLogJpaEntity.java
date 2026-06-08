package com.foodscanner.infrastructure.persistence.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Слой: infrastructure (JPA).
 * Хранилище клиентских логов. metadata — jsonb. Запись-только (read для домена не требуется).
 */
@Entity
@Table(schema = "food_catalog", name = "client_logs")
public class ClientLogJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "client_log_id", length = 128)
    private String clientLogId;

    @Column(name = "correlation_id", columnDefinition = "uuid")
    private UUID correlationId;

    @Column(name = "request_id", columnDefinition = "uuid")
    private UUID requestId;

    @Column(name = "`timestamp`", nullable = false)
    private Instant timestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(length = 128)
    private String event;

    @Column(columnDefinition = "text")
    private String message;

    @Column(length = 128)
    private String screen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(length = 64)
    private String barcode;

    @Column(name = "draft_id", columnDefinition = "uuid")
    private UUID draftId;

    @Column(name = "catalog_entry_id", columnDefinition = "uuid")
    private UUID catalogEntryId;

    @Column(name = "photo_id", columnDefinition = "uuid")
    private UUID photoId;

    @Column(name = "api_method", length = 16)
    private String apiMethod;

    @Column(name = "api_path", length = 256)
    private String apiPath;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "client_version", length = 64)
    private String clientVersion;

    @Column(name = "pwa_version", length = 64)
    private String pwaVersion;

    protected ClientLogJpaEntity() {}

    public ClientLogJpaEntity(UUID id, UUID contributorId, UUID sessionId, String clientLogId,
                              UUID correlationId, UUID requestId, Instant timestamp, Instant receivedAt,
                              String level, String category, String event, String message, String screen,
                              Map<String, Object> metadata, Long durationMs, String stackTrace, String barcode,
                              UUID draftId, UUID catalogEntryId, UUID photoId, String apiMethod, String apiPath,
                              Integer httpStatus, String clientVersion, String pwaVersion) {
        this.id = id;
        this.contributorId = contributorId;
        this.sessionId = sessionId;
        this.clientLogId = clientLogId;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.receivedAt = receivedAt;
        this.level = level;
        this.category = category;
        this.event = event;
        this.message = message;
        this.screen = screen;
        this.metadata = metadata;
        this.durationMs = durationMs;
        this.stackTrace = stackTrace;
        this.barcode = barcode;
        this.draftId = draftId;
        this.catalogEntryId = catalogEntryId;
        this.photoId = photoId;
        this.apiMethod = apiMethod;
        this.apiPath = apiPath;
        this.httpStatus = httpStatus;
        this.clientVersion = clientVersion;
        this.pwaVersion = pwaVersion;
    }
}
