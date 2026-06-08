package com.foodscanner.domain.model.telemetry;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Value Object.
 *
 * Снимок клиентской сессии (устройство/браузер/окружение). На один sessionId —
 * одна строка; повторный POST обновляет last_seen_at.
 */
public record ClientSession(
        UUID id,
        UUID contributorId,
        UUID sessionId,
        Instant startedAt,
        Instant lastSeenAt,
        String clientVersion,
        String pwaVersion,
        String browser,
        String os,
        String deviceType,
        String language,
        String timezone,
        Integer screenWidth,
        Integer screenHeight,
        Integer hardwareConcurrency,
        Double deviceMemory,
        String networkStatus,
        Boolean standalone
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID contributorId;
        private UUID sessionId;
        private Instant startedAt;
        private Instant lastSeenAt;
        private String clientVersion;
        private String pwaVersion;
        private String browser;
        private String os;
        private String deviceType;
        private String language;
        private String timezone;
        private Integer screenWidth;
        private Integer screenHeight;
        private Integer hardwareConcurrency;
        private Double deviceMemory;
        private String networkStatus;
        private Boolean standalone;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder contributorId(UUID v) { this.contributorId = v; return this; }
        public Builder sessionId(UUID v) { this.sessionId = v; return this; }
        public Builder startedAt(Instant v) { this.startedAt = v; return this; }
        public Builder lastSeenAt(Instant v) { this.lastSeenAt = v; return this; }
        public Builder clientVersion(String v) { this.clientVersion = v; return this; }
        public Builder pwaVersion(String v) { this.pwaVersion = v; return this; }
        public Builder browser(String v) { this.browser = v; return this; }
        public Builder os(String v) { this.os = v; return this; }
        public Builder deviceType(String v) { this.deviceType = v; return this; }
        public Builder language(String v) { this.language = v; return this; }
        public Builder timezone(String v) { this.timezone = v; return this; }
        public Builder screenWidth(Integer v) { this.screenWidth = v; return this; }
        public Builder screenHeight(Integer v) { this.screenHeight = v; return this; }
        public Builder hardwareConcurrency(Integer v) { this.hardwareConcurrency = v; return this; }
        public Builder deviceMemory(Double v) { this.deviceMemory = v; return this; }
        public Builder networkStatus(String v) { this.networkStatus = v; return this; }
        public Builder standalone(Boolean v) { this.standalone = v; return this; }

        public ClientSession build() {
            return new ClientSession(id, contributorId, sessionId, startedAt, lastSeenAt, clientVersion,
                pwaVersion, browser, os, deviceType, language, timezone, screenWidth, screenHeight,
                hardwareConcurrency, deviceMemory, networkStatus, standalone);
        }
    }
}
