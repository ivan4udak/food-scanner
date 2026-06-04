package com.foodscanner.infrastructure.persistence.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PhotoObjectJpaRepository extends JpaRepository<PhotoObjectJpaEntity, String> {
    @Modifying
    @Query("delete from PhotoObjectJpaEntity p where p.objectKey = :objectKey")
    int deleteByObjectKey(String objectKey);
}
