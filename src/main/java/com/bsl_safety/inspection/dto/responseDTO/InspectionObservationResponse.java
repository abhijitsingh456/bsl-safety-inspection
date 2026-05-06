package com.bsl_safety.inspection.dto.responseDTO;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InspectionObservationResponse {
    private UUID observationId;
    private UUID userId;
    private LocalDate inspectionDate;
    private String category;
    private String department;
    private String subDepartment;
    private String location;
    private String observation;
    private String complianceStatus;
    private LocalDate targetDate;
    private List<String> observationPhotoUrl;
    private List<String> compliedPhotoUrl;
    private String photoUploadStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
