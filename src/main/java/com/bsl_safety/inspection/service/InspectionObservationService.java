package com.bsl_safety.inspection.service;

import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InspectionObservationService {
    InspectionObservationResponse createInspectionObservation(InspectionObservationRequest inspectionObservationRequest,
                                                              List<MultipartFile> inspectionPhotos,
                                                              List<MultipartFile> compliedPhotos);
}
