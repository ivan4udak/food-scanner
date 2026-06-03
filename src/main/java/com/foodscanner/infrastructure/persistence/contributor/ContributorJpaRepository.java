package com.foodscanner.infrastructure.persistence.contributor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContributorJpaRepository
        extends JpaRepository<ContributorJpaEntity, UUID> {
    boolean existsByNickname(String nickname);
    Optional<ContributorJpaEntity> findByUsername(String username);

    /** Удаляет аккаунты, у которых истекло окно восстановления (пароль так и не задан). */
    @Modifying
    @Query("delete from ContributorJpaEntity c " +
           "where c.passwordHash is null and c.resetPasswordUntil is not null " +
           "and c.resetPasswordUntil < :now")
    int deleteExpiredRecoveries(Instant now);
}
