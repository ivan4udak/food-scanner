package com.foodscanner.domain.model.telemetry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Value Object (append-only телеметрия).
 *
 * Одна запись клиентского лога, принятая backend-ом. Метаданные хранятся как
 * Map (домен не знает про JSON/Jackson — сериализация в jsonb в infrastructure).
 * Секреты уже замаскированы в application перед созданием доменного объекта.
 */
public record ClientLogEntry(
        UUID id,
        UUID contributorId,
        UUID sessionId,
        String clientLogId,
        UUID correlationId,
        UUID requestId,
        Instant timestamp,
        Instant receivedAt,
        String level,
        String category,
        String event,
        String message,
        String screen,
        Map<String, Object> metadata,
        Long durationMs,
        String stackTrace,
        String barcode,
        UUID draftId,
        UUID catalogEntryId,
        UUID photoId,
        String apiMethod,
        String apiPath,
        Integer httpStatus,
        String clientVersion,
        String pwaVersion
) {
    public static Builder builder() {
        return new Builder();
    }

    /** Builder — защищает от позиционных ошибок при большом числе полей. */
    public static final class Builder {
        private UUID id;
        private UUID contributorId;
        private UUID sessionId;
        private String clientLogId;
        private UUID correlationId;
        private UUID requestId;
        private Instant timestamp;
        private Instant receivedAt;
        private String level;
        private String category;
        private String event;
        private String message;
        private String screen;
        private Map<String, Object> metadata;
        private Long durationMs;
        private String stackTrace;
        private String barcode;
        private UUID draftId;
        private UUID catalogEntryId;
        private UUID photoId;
        private String apiMethod;
        private String apiPath;
        private Integer httpStatus;
        private String clientVersion;
        private String pwaVersion;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder contributorId(UUID v) { this.contributorId = v; return this; }
        public Builder sessionId(UUID v) { this.sessionId = v; return this; }
        public Builder clientLogId(String v) { this.clientLogId = v; return this; }
        public Builder correlationId(UUID v) { this.correlationId = v; return this; }
        public Builder requestId(UUID v) { this.requestId = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder receivedAt(Instant v) { this.receivedAt = v; return this; }
        public Builder level(String v) { this.level = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder event(String v) { this.event = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder screen(String v) { this.screen = v; return this; }
        public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }
        public Builder durationMs(Long v) { this.durationMs = v; return this; }
        public Builder stackTrace(String v) { this.stackTrace = v; return this; }
        public Builder barcode(String v) { this.barcode = v; return this; }
        public Builder draftId(UUID v) { this.draftId = v; return this; }
        public Builder catalogEntryId(UUID v) { this.catalogEntryId = v; return this; }
        public Builder photoId(UUID v) { this.photoId = v; return this; }
        public Builder apiMethod(String v) { this.apiMethod = v; return this; }
        public Builder apiPath(String v) { this.apiPath = v; return this; }
        public Builder httpStatus(Integer v) { this.httpStatus = v; return this; }
        public Builder clientVersion(String v) { this.clientVersion = v; return this; }
        public Builder pwaVersion(String v) { this.pwaVersion = v; return this; }

        public ClientLogEntry build() {
            return new ClientLogEntry(id, contributorId, sessionId, clientLogId, correlationId,
                requestId, timestamp, receivedAt, level, category, event, message, screen, metadata,
                durationMs, stackTrace, barcode, draftId, catalogEntryId, photoId, apiMethod, apiPath,
                httpStatus, clientVersion, pwaVersion);
        }
    }
}
