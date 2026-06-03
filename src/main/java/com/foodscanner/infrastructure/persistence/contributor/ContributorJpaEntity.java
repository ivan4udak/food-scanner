package com.foodscanner.infrastructure.persistence.contributor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Слой: infrastructure
 * JPA-сущность контрибьютора (отделена от доменного Contributor).
 * vNext: добавлены поля аутентификации.
 */
@Entity
@Table(schema = "food_catalog", name = "contributors")
public class ContributorJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "reset_password_until")
    private Instant resetPasswordUntil;

    @Column(name = "completed_catalog_count", nullable = false)
    private int completedCatalogCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContributorJpaEntity() {}

    public ContributorJpaEntity(UUID id, String nickname, String username, String passwordHash,
                                int failedLoginAttempts, Instant lockedUntil, Instant resetPasswordUntil,
                                int completedCatalogCount, Instant createdAt, Instant updatedAt) {
        this.id                    = id;
        this.nickname              = nickname;
        this.username              = username;
        this.passwordHash          = passwordHash;
        this.failedLoginAttempts   = failedLoginAttempts;
        this.lockedUntil           = lockedUntil;
        this.resetPasswordUntil    = resetPasswordUntil;
        this.completedCatalogCount = completedCatalogCount;
        this.createdAt             = createdAt;
        this.updatedAt             = updatedAt;
    }

    public UUID    getId()                    { return id; }
    public String  getNickname()              { return nickname; }
    public String  getUsername()              { return username; }
    public String  getPasswordHash()          { return passwordHash; }
    public int     getFailedLoginAttempts()   { return failedLoginAttempts; }
    public Instant getLockedUntil()           { return lockedUntil; }
    public Instant getResetPasswordUntil()    { return resetPasswordUntil; }
    public int     getCompletedCatalogCount() { return completedCatalogCount; }
    public Instant getCreatedAt()             { return createdAt; }
    public Instant getUpdatedAt()             { return updatedAt; }
}
