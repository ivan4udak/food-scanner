package com.foodscanner.application.query;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: application (query-объект).
 * Фильтр просмотра клиентских логов в админке. null-поля игнорируются.
 */
public record AdminLogFilter(
        UUID contributorId,
        UUID sessionId,
        String level,
        String category,
        String event,
        String barcode,
        String screen,
        Instant dateFrom,
        Instant dateTo,
        int limit,
        int offset
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID contributorId;
        private UUID sessionId;
        private String level;
        private String category;
        private String event;
        private String barcode;
        private String screen;
        private Instant dateFrom;
        private Instant dateTo;
        private int limit = 100;
        private int offset = 0;

        public Builder contributorId(UUID v) { this.contributorId = v; return this; }
        public Builder sessionId(UUID v) { this.sessionId = v; return this; }
        public Builder level(String v) { this.level = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder event(String v) { this.event = v; return this; }
        public Builder barcode(String v) { this.barcode = v; return this; }
        public Builder screen(String v) { this.screen = v; return this; }
        public Builder dateFrom(Instant v) { this.dateFrom = v; return this; }
        public Builder dateTo(Instant v) { this.dateTo = v; return this; }
        public Builder limit(int v) { this.limit = v; return this; }
        public Builder offset(int v) { this.offset = v; return this; }

        public AdminLogFilter build() {
            return new AdminLogFilter(contributorId, sessionId, level, category, event,
                barcode, screen, dateFrom, dateTo, limit, offset);
        }
    }
}
