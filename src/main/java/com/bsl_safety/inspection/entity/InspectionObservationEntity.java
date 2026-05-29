package com.bsl_safety.inspection.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name="inspection_observation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"observationHash"})
)
public class InspectionObservationEntity {

    @Id
    @GeneratedValue
    @Column(name="observation_id")
    private UUID observationId;

    @Column(name="user_id")
    private UUID userId;

    @Column(name="inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name="category", nullable = false)
    private String category;

    @Column(name="department", nullable = false)
    private String department;

    @Column(name="sub_department")
    private String subDepartment;

    @Column(name="location")
    private String location;

    @Column(name="observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name="compliance_status")
    private String complianceStatus;

    @Column(name="target_date")
    private LocalDate targetDate;

    @Column(name="to_be_included_in_dispatcher")
    private String toBeIncludedInDispatcher;

    @Column(name="recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name="discussed_with")
    private String discussedWith;

    @Column(name="inspection_photo_url")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> inspectionPhotoUrl;

    @Column(name="complied_photo_url")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> compliedPhotoUrl;

    @Column(name="photo_upload_status")
    private String photoUploadStatus;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    @Column(name="observation_hash")
    private String observationHash;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;   //  SAME VALUE ON FIRST INSERT
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }
}
