package com.bsl_safety.inspection.controller;


import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import com.bsl_safety.inspection.service.InspectionObservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

}
