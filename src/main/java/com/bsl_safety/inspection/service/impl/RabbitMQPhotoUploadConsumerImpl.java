package com.bsl_safety.inspection.service.impl;

import com.bsl_safety.inspection.configuration.RabbitMQConfig;
import com.bsl_safety.inspection.dto.requestDTO.RabbitMQPhotoUploadMessage;
import com.bsl_safety.inspection.entity.InspectionObservationEntity;
import com.bsl_safety.inspection.exception.ResourceNotFoundException;
import com.bsl_safety.inspection.repository.InspectionObservationRepository;
import com.bsl_safety.inspection.service.CloudinaryUploadService;
import com.bsl_safety.inspection.service.RabbitMQPhotoUploadConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPhotoUploadConsumerImpl implements RabbitMQPhotoUploadConsumer {
    private final CloudinaryUploadService cloudinaryUploadService;
    private final InspectionObservationRepository inspectionObservationRepository;

    @Override
    @RabbitListener(queues = RabbitMQConfig.PHOTO_UPLOAD_QUEUE)
    public void handlePhotoUpload(RabbitMQPhotoUploadMessage message) throws IOException {

        log.info("Received photo upload message for inspection id = {}", message.getInspectionId());

        try {
            List<String> inspectionPhotoUrls = new ArrayList<>();
            List<String> compliedPhotoUrls = new ArrayList<>();

            if (!message.getInspectionPhotoPaths().isEmpty()) {
                inspectionPhotoUrls = cloudinaryUploadService.uploadFromPaths(message.getInspectionPhotoPaths());
                cleanupTempFiles(message.getInspectionPhotoPaths());
            }

            if (!message.getCompliedPhotoPaths().isEmpty()) {
                compliedPhotoUrls = cloudinaryUploadService.uploadFromPaths(message.getCompliedPhotoPaths());
                cleanupTempFiles(message.getCompliedPhotoPaths());
            }

            InspectionObservationEntity observation = inspectionObservationRepository.findById(message.getInspectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Observation does not exist."));

            for (String url : inspectionPhotoUrls) {
                observation.getInspectionPhotoUrl().add(url);
            }

            for (String url : compliedPhotoUrls) {
                observation.getCompliedPhotoUrl().add(url);
            }

            observation.setPhotoUploadStatus("UPLOAD_COMPLETE");
            inspectionObservationRepository.save(observation);

            log.info("Photo upload complete for inspection id = {}", message.getInspectionId());

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("File size too large")) {
                // Don't retry - mark as failed and move on
                log.error("Photo too large for inspection id = {}, marking as failed: {}",
                        message.getInspectionId(), e.getMessage());
                try {
                    InspectionObservationEntity observation = inspectionObservationRepository
                            .findById(message.getInspectionId()).orElse(null);
                    if (observation != null) {
                        observation.setPhotoUploadStatus("UPLOAD_FAILED_FILE_TOO_LARGE");
                        inspectionObservationRepository.save(observation);
                    }
                } catch (Exception dbEx) {
                    log.error("Failed to update status for inspection id = {}", message.getInspectionId());
                }
                // Return normally - RabbitMQ will acknowledge and NOT retry
                return;
            }
            // For other errors, still rethrow (network issues etc. are worth retrying)
            log.error("Photo upload failed for inspection id = {}", message.getInspectionId());
            throw e;
        }
    }

    private void cleanupTempFiles(List<String> paths){
        if(paths == null || paths.isEmpty()){
            return;
        }

        paths.forEach(path->{
            try{
                Files.deleteIfExists(Paths.get(path));
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path);
            }
        });
    }
}
