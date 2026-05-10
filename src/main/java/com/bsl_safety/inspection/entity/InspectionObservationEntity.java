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
    @Column(name="observationId")
    private UUID observationId;

    @Column(name="userId")
    private UUID userId;

    @Column(name="inspectionDate", nullable = false)
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

    @Column(name="complianceStatus")
    private String complianceStatus;

    @Column(name="targetDate")
    private LocalDate targetDate;

    @Column(name="toBeIncludedInDispatcher")
    private String toBeIncludedInDispatcher;

    @Column(name="recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name="discussedWith")
    private String discussedWith;

    @Column(name="inspectionPhotoUrl")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> inspectionPhotoUrl;

    @Column(name="compliedPhotoUrl")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> compliedPhotoUrl;

    @Column(name="photoUploadStatus")
    private String photoUploadStatus;

    @Column(name = "isDeleted")
    private Boolean isDeleted;

    @Column(name="createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name="updatedAt")
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
