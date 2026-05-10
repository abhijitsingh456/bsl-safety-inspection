package com.bsl_safety.inspection.repository;

import com.bsl_safety.inspection.entity.InspectionObservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionObservationRepository extends JpaRepository<InspectionObservationEntity, UUID> {
    Boolean existsByObservationHash(String hash);
    Optional<InspectionObservationEntity> findByObservationId(UUID id);

    @Query(value = """
        SELECT e FROM InspectionObservationEntity e
        WHERE e.isDeleted = false
        AND (:departments IS NULL OR e.department IN :departments)
        AND (:categories IS NULL OR e.category IN :categories)
        AND (:complianceStatus IS NULL OR e.complianceStatus IN :complianceStatus)
        AND (:inspectionStartDate IS NULL OR e.inspectionDate >= :inspectionStartDate)
        AND (:inspectionEndDate IS NULL OR e.inspectionDate <= :inspectionEndDate)
        AND (:targetStartDate IS NULL OR e.targetDate >= :targetStartDate)
        AND (:targetEndDate IS NULL OR e.targetDate <= :targetEndDate)
        """)
    Page<InspectionObservationEntity> searchObservations(
            @Param("departments") List<String> departments,
            @Param("categories") List<String> categories,
            @Param("complianceStatus") List<String> complianceStatus,
            @Param("inspectionStartDate") LocalDate inspectionStartDate,
            @Param("inspectionEndDate") LocalDate inspectionEndDate,
            @Param("targetStartDate") LocalDate targetStartDate,
            @Param("targetEndDate") LocalDate targetEndDate,
            Pageable pageable
    );
}