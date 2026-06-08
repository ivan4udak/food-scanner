package com.foodscanner.application.command;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Слой: application (команда).
 * Партия клиентских логов, принятая backend-ом. Маскировка применяется в сервисе.
 */
public record IngestClientLogsCommand(
        UUID contributorId,
        UUID sessionId,
        String clientVersion,
        String pwaVersion,
        Instant receivedAt,
        List<LogLine> logs
) {
    /** Одна строка клиентского лога (до маскировки/маппинга в доменную модель). */
    public record LogLine(
            String clientLogId,
            Instant timestamp,
            String level,
            String category,
            String event,
            String message,
            String screen,
            Map<String, Object> metadata,
            Long durationMs,
            String stackTrace,
            UUID correlationId,
            UUID requestId,
            String barcode,
            UUID draftId,
            UUID catalogEntryId,
            UUID photoId,
            String apiMethod,
            String apiPath,
            Integer httpStatus
    ) {
        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String clientLogId;
            private Instant timestamp;
            private String level;
            private String category;
            private String event;
            private String message;
            private String screen;
            private Map<String, Object> metadata;
            private Long durationMs;
            private String stackTrace;
            private UUID correlationId;
            private UUID requestId;
            private String barcode;
            private UUID draftId;
            private UUID catalogEntryId;
            private UUID photoId;
            private String apiMethod;
            private String apiPath;
            private Integer httpStatus;

            public Builder clientLogId(String v) { this.clientLogId = v; return this; }
            public Builder timestamp(Instant v) { this.timestamp = v; return this; }
            public Builder level(String v) { this.level = v; return this; }
            public Builder category(String v) { this.category = v; return this; }
            public Builder event(String v) { this.event = v; return this; }
            public Builder message(String v) { this.message = v; return this; }
            public Builder screen(String v) { this.screen = v; return this; }
            public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }
            public Builder durationMs(Long v) { this.durationMs = v; return this; }
            public Builder stackTrace(String v) { this.stackTrace = v; return this; }
            public Builder correlationId(UUID v) { this.correlationId = v; return this; }
            public Builder requestId(UUID v) { this.requestId = v; return this; }
            public Builder barcode(String v) { this.barcode = v; return this; }
            public Builder draftId(UUID v) { this.draftId = v; return this; }
            public Builder catalogEntryId(UUID v) { this.catalogEntryId = v; return this; }
            public Builder photoId(UUID v) { this.photoId = v; return this; }
            public Builder apiMethod(String v) { this.apiMethod = v; return this; }
            public Builder apiPath(String v) { this.apiPath = v; return this; }
            public Builder httpStatus(Integer v) { this.httpStatus = v; return this; }

            public LogLine build() {
                return new LogLine(clientLogId, timestamp, level, category, event, message, screen,
                    metadata, durationMs, stackTrace, correlationId, requestId, barcode, draftId,
                    catalogEntryId, photoId, apiMethod, apiPath, httpStatus);
            }
        }
    }
}
