package com.foodscanner.domain.model.telemetry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Value Object (append-only бизнес-событие сервера).
 *
 * Значимое серверное событие (не весь лог-шум). Связывается с клиентскими логами
 * через correlationId/requestId для сквозной диагностики.
 */
public record ServerEvent(
        UUID id,
        Instant occurredAt,
        String level,
        String event,
        UUID correlationId,
        UUID requestId,
        UUID contributorId,
        String username,
        UUID sessionId,
        String method,
        String path,
        Integer httpStatus,
        Long durationMs,
        String useCase,
        String barcode,
        UUID draftId,
        UUID catalogEntryId,
        UUID photoId,
        String errorCode,
        String errorMessage,
        String exceptionClass,
        Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private Instant occurredAt;
        private String level;
        private String event;
        private UUID correlationId;
        private UUID requestId;
        private UUID contributorId;
        private String username;
        private UUID sessionId;
        private String method;
        private String path;
        private Integer httpStatus;
        private Long durationMs;
        private String useCase;
        private String barcode;
        private UUID draftId;
        private UUID catalogEntryId;
        private UUID photoId;
        private String errorCode;
        private String errorMessage;
        private String exceptionClass;
        private Map<String, Object> metadata;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder occurredAt(Instant v) { this.occurredAt = v; return this; }
        public Builder level(String v) { this.level = v; return this; }
        public Builder event(String v) { this.event = v; return this; }
        public Builder correlationId(UUID v) { this.correlationId = v; return this; }
        public Builder requestId(UUID v) { this.requestId = v; return this; }
        public Builder contributorId(UUID v) { this.contributorId = v; return this; }
        public Builder username(String v) { this.username = v; return this; }
        public Builder sessionId(UUID v) { this.sessionId = v; return this; }
        public Builder method(String v) { this.method = v; return this; }
        public Builder path(String v) { this.path = v; return this; }
        public Builder httpStatus(Integer v) { this.httpStatus = v; return this; }
        public Builder durationMs(Long v) { this.durationMs = v; return this; }
        public Builder useCase(String v) { this.useCase = v; return this; }
        public Builder barcode(String v) { this.barcode = v; return this; }
        public Builder draftId(UUID v) { this.draftId = v; return this; }
        public Builder catalogEntryId(UUID v) { this.catalogEntryId = v; return this; }
        public Builder photoId(UUID v) { this.photoId = v; return this; }
        public Builder errorCode(String v) { this.errorCode = v; return this; }
        public Builder errorMessage(String v) { this.errorMessage = v; return this; }
        public Builder exceptionClass(String v) { this.exceptionClass = v; return this; }
        public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }

        public ServerEvent build() {
            return new ServerEvent(id, occurredAt, level, event, correlationId, requestId, contributorId,
                username, sessionId, method, path, httpStatus, durationMs, useCase, barcode, draftId,
                catalogEntryId, photoId, errorCode, errorMessage, exceptionClass, metadata);
        }
    }
}
