package com.foodscanner.infrastructure.persistence.extraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductExtractionJobJpaRepository extends JpaRepository<ProductExtractionJobJpaEntity, UUID> {
}
