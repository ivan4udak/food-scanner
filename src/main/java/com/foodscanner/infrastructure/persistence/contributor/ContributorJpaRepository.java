package com.foodscanner.infrastructure.persistence.contributor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContributorJpaRepository
        extends JpaRepository<ContributorJpaEntity, UUID> {
    boolean existsByNickname(String nickname);
}
