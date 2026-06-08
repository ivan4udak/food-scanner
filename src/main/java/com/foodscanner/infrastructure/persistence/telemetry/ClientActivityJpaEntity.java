package com.foodscanner.infrastructure.persistence.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Слой: infrastructure (JPA).
 * Событие активности клиента (heartbeat).
 */
@Entity
@Table(schema = "food_catalog", name = "client_activity")
public class ClientActivityJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(length = 128)
    private String screen;

    @Column
    private Boolean online;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ClientActivityJpaEntity() {}

    public ClientActivityJpaEntity(UUID id, UUID contributorId, UUID sessionId, String screen,
                                   Boolean online, Instant occurredAt, Instant receivedAt) {
        this.id = id;
        this.contributorId = contributorId;
        this.sessionId = sessionId;
        this.screen = screen;
        this.online = online;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
    }
}
