package com.foodscanner.infrastructure.persistence.ocr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OcrJobJpaRepository extends JpaRepository<OcrJobJpaEntity, UUID> {
    List<OcrJobJpaEntity> findByDraftId(UUID draftId);
}
