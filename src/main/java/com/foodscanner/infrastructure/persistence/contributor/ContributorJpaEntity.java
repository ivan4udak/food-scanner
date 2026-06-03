package com.foodscanner.infrastructure.persistence.contributor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Слой: infrastructure
 *
 * JPA-сущность — намеренно отделена от доменного Contributor.
 * Схема БД и доменная модель эволюционируют независимо.
 *
 * @PrePersist / @PreUpdate управляют updated_at на уровне инфраструктуры —
 * не засоряя доменную модель JPA-деталями.
 */
@Entity
@Table(schema = "food_catalog", name = "contributors")
public class ContributorJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(name = "completed_catalog_count", nullable = false)
    private int completedCatalogCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContributorJpaEntity() {}

    public ContributorJpaEntity(UUID id, String nickname, int completedCatalogCount,
                                Instant createdAt, Instant updatedAt) {
        this.id                    = id;
        this.nickname              = nickname;
        this.completedCatalogCount = completedCatalogCount;
        this.createdAt             = createdAt;
        this.updatedAt             = updatedAt;
    }

    public UUID    getId()                    { return id; }
    public String  getNickname()              { return nickname; }
    public int     getCompletedCatalogCount() { return completedCatalogCount; }
    public Instant getCreatedAt()             { return createdAt; }
    public Instant getUpdatedAt()             { return updatedAt; }
}
