package com.bsl_safety.inspection.controller;


import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import com.bsl_safety.inspection.service.InspectionObservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inspection/")
@RequiredArgsConstructor
public class InspectionObservationController {

    private final InspectionObservationService inspectionObservationService;

    @PostMapping(consumes="multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public InspectionObservationResponse createInspection(@Valid @RequestPart("observation") InspectionObservationRequest inspectionObservationRequest,
                                                          @RequestPart(value = "inspection_photos", required = false) List<MultipartFile> inspectionPhotos,
                                                          @RequestPart(value = "complied_photos", required = false) List<MultipartFile> compliedPhotos){



        return inspectionObservationService.createInspectionObservation(inspectionObservationRequest, inspectionPhotos, compliedPhotos);

    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<InspectionObservationResponse> findInspectionObservation(
            @RequestParam(required = false) List<String> department,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> complianceStatus,
            @RequestParam(required = false) LocalDate inspectionStartDate,
            @RequestParam(required = false) LocalDate inspectionEndDate,
            @RequestParam(required = false) LocalDate targetStartDate,
            @RequestParam(required = false) LocalDate targetEndDate,
            @RequestParam(required = false) LocalDate updatedOn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size){


            return inspectionObservationService.findInspectionObservation(department, category,
                    complianceStatus, inspectionStartDate, inspectionEndDate, targetStartDate, targetEndDate,
                    updatedOn, PageRequest.of(page, size));
    }

}
