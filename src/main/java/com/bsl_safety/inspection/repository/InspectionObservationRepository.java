package com.bsl_safety.inspection.repository;

import com.bsl_safety.inspection.entity.InspectionObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InspectionObservationRepository extends JpaRepository<InspectionObservationEntity, UUID> {
    Boolean existsByObservationHash(String hash);
    Optional<InspectionObservationEntity> findByObservationId(UUID id);
}
