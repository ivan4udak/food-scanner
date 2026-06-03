package com.foodscanner.infrastructure.security;

import com.foodscanner.infrastructure.persistence.contributor.ContributorJpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Слой: infrastructure
 * Удаляет аккаунты, не задавшие новый пароль в течение окна восстановления (5 мин).
 * Запускается раз в минуту.
 */
@Component
public class RecoveryCleanupJob {

    private final ContributorJpaRepository jpa;

    public RecoveryCleanupJob(ContributorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void purgeExpiredRecoveries() {
        jpa.deleteExpiredRecoveries(Instant.now());
    }
}
