package com.bsl_safety.inspection.dto.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionObservationRequest {
    @NotNull
    private LocalDate inspectionDate;

    @NotBlank
    private String category;

    @NotBlank
    private String department;

    private String subDepartment;

    private String location;

    private String observation;

    private String complianceStatus;

    private LocalDate targetDate;

}
