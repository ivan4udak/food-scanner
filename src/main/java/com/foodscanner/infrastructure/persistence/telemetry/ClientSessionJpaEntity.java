package com.foodscanner.infrastructure.persistence.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: infrastructure (JPA).
 * Клиентская сессия — одна строка на session_id.
 */
@Entity
@Table(schema = "food_catalog", name = "client_sessions")
public class ClientSessionJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(name = "session_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "client_version", length = 64)
    private String clientVersion;

    @Column(name = "pwa_version", length = 64)
    private String pwaVersion;

    @Column(length = 128)
    private String browser;

    @Column(length = 128)
    private String os;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(length = 32)
    private String language;

    @Column(length = 128)
    private String timezone;

    @Column(name = "screen_width")
    private Integer screenWidth;

    @Column(name = "screen_height")
    private Integer screenHeight;

    @Column(name = "hardware_concurrency")
    private Integer hardwareConcurrency;

    @Column(name = "device_memory")
    private Double deviceMemory;

    @Column(name = "network_status", length = 32)
    private String networkStatus;

    @Column
    private Boolean standalone;

    protected ClientSessionJpaEntity() {}

    public ClientSessionJpaEntity(UUID id, UUID contributorId, UUID sessionId, Instant startedAt,
                                  Instant lastSeenAt, String clientVersion, String pwaVersion, String browser,
                                  String os, String deviceType, String language, String timezone,
                                  Integer screenWidth, Integer screenHeight, Integer hardwareConcurrency,
                                  Double deviceMemory, String networkStatus, Boolean standalone) {
        this.id = id;
        this.contributorId = contributorId;
        this.sessionId = sessionId;
        this.startedAt = startedAt;
        this.lastSeenAt = lastSeenAt;
        this.clientVersion = clientVersion;
        this.pwaVersion = pwaVersion;
        this.browser = browser;
        this.os = os;
        this.deviceType = deviceType;
        this.language = language;
        this.timezone = timezone;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.hardwareConcurrency = hardwareConcurrency;
        this.deviceMemory = deviceMemory;
        this.networkStatus = networkStatus;
        this.standalone = standalone;
    }

    public UUID getId() { return id; }
    public UUID getContributorId() { return contributorId; }
    public UUID getSessionId() { return sessionId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public String getClientVersion() { return clientVersion; }
    public String getPwaVersion() { return pwaVersion; }
    public String getBrowser() { return browser; }
    public String getOs() { return os; }
    public String getDeviceType() { return deviceType; }
    public String getLanguage() { return language; }
    public String getTimezone() { return timezone; }
    public Integer getScreenWidth() { return screenWidth; }
    public Integer getScreenHeight() { return screenHeight; }
    public Integer getHardwareConcurrency() { return hardwareConcurrency; }
    public Double getDeviceMemory() { return deviceMemory; }
    public String getNetworkStatus() { return networkStatus; }
    public Boolean getStandalone() { return standalone; }

    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
