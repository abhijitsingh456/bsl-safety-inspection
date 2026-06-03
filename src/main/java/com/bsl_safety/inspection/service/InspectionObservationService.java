package com.bsl_safety.inspection.service;

import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InspectionObservationService {
    InspectionObservationResponse createInspectionObservation(InspectionObservationRequest inspectionObservationRequest,
                                                              List<MultipartFile> inspectionPhotos,
                                                              List<MultipartFile> compliedPhotos);


    InspectionObservationResponse updateInspectionObservation(UUID observationID,
                                                              InspectionObservationRequest inspectionObservationRequest,
                                                              List<MultipartFile> inspectionPhotos,
                                                              List<MultipartFile> compliedPhotos);


    Page<InspectionObservationResponse> findInspectionObservation(List<String> department,
                                                                  List<String> category,
                                                                  List<String> complianceStatus,
                                                                  LocalDate inspectionStartDate,
                                                                  LocalDate inspectionEndDate,
                                                                  LocalDate targetStartDate,
                                                                  LocalDate targetEndDate,
                                                                  LocalDate updatedOn,
                                                                  Pageable pageable);
}

