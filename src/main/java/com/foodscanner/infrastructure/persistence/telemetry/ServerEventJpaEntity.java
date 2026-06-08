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
 * Серверное бизнес-событие. metadata — jsonb. Запись-только.
 */
@Entity
@Table(schema = "food_catalog", name = "server_events")
public class ServerEventJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 128)
    private String event;

    @Column(name = "correlation_id", columnDefinition = "uuid")
    private UUID correlationId;

    @Column(name = "request_id", columnDefinition = "uuid")
    private UUID requestId;

    @Column(name = "contributor_id", columnDefinition = "uuid")
    private UUID contributorId;

    @Column(length = 128)
    private String username;

    @Column(name = "session_id", columnDefinition = "uuid")
    private UUID sessionId;

    @Column(length = 16)
    private String method;

    @Column(length = 256)
    private String path;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "use_case", length = 128)
    private String useCase;

    @Column(length = 64)
    private String barcode;

    @Column(name = "draft_id", columnDefinition = "uuid")
    private UUID draftId;

    @Column(name = "catalog_entry_id", columnDefinition = "uuid")
    private UUID catalogEntryId;

    @Column(name = "photo_id", columnDefinition = "uuid")
    private UUID photoId;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "exception_class", length = 256)
    private String exceptionClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected ServerEventJpaEntity() {}

    public ServerEventJpaEntity(UUID id, Instant occurredAt, String level, String event, UUID correlationId,
                                UUID requestId, UUID contributorId, String username, UUID sessionId,
                                String method, String path, Integer httpStatus, Long durationMs, String useCase,
                                String barcode, UUID draftId, UUID catalogEntryId, UUID photoId, String errorCode,
                                String errorMessage, String exceptionClass, Map<String, Object> metadata) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.level = level;
        this.event = event;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.contributorId = contributorId;
        this.username = username;
        this.sessionId = sessionId;
        this.method = method;
        this.path = path;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.useCase = useCase;
        this.barcode = barcode;
        this.draftId = draftId;
        this.catalogEntryId = catalogEntryId;
        this.photoId = photoId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.exceptionClass = exceptionClass;
        this.metadata = metadata;
    }
}
