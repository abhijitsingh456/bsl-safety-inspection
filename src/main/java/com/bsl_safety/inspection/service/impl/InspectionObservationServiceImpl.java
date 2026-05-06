package com.bsl_safety.inspection.service.impl;

import com.bsl_safety.inspection.configuration.RabbitMQConfig;
import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.requestDTO.RabbitMQPhotoUploadMessage;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import com.bsl_safety.inspection.entity.InspectionObservationEntity;
import com.bsl_safety.inspection.exception.DataIntegrityException;
import com.bsl_safety.inspection.repository.InspectionObservationRepository;
import com.bsl_safety.inspection.service.CloudinaryUploadService;
import com.bsl_safety.inspection.service.InspectionObservationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class InspectionObservationServiceImpl implements InspectionObservationService {

    private final InspectionObservationRepository inspectionObservationRepository;
    private final CloudinaryUploadService cloudinaryUploadService;
    private final AmqpTemplate amqpTemplate;

    @Override
    public InspectionObservationResponse createInspectionObservation(InspectionObservationRequest request,
                                                                     List<MultipartFile> inspectionPhotos,
                                                                     List<MultipartFile> compliedPhotos){

        //stop duplicate entries
        String raw = String.format(
                "%s|%s|%s|%s",
                request.getInspectionDate().toString(),
                request.getDepartment().trim().toLowerCase(),
                request.getLocation().trim().toLowerCase(),
                request.getObservation().trim().toLowerCase()
        );
        String hash = DigestUtils.sha256Hex(raw);
        if(inspectionObservationRepository.existsByObservationHash(hash)){
            throw new DataIntegrityException("Observation already exists");
        }

        InspectionObservationEntity observation = new InspectionObservationEntity();
        observation.setInspectionDate(request.getInspectionDate());
        observation.setCategory(request.getCategory());
        observation.setDepartment(request.getDepartment());
        observation.setSubDepartment(request.getSubDepartment());
        observation.setLocation(request.getLocation());
        observation.setObservation(request.getObservation());
        observation.setComplianceStatus(request.getComplianceStatus());
        observation.setTargetDate(request.getTargetDate());
        observation.setPhotoUploadStatus("PENDING_UPLOAD");
        observation.setIsDeleted(Boolean.FALSE);
        observation.setObservationHash(hash);
        InspectionObservationEntity observationSaved = inspectionObservationRepository.save(observation);

        //save photos locally in temp files, so that their paths can be sent in the RabbitMQ message
        //This is done to avoid sending heavy actual photo bytes in RabbitMQ messages
        List<String> inspectionPhotoPaths = saveToTempFile(inspectionPhotos);
        List<String> compliedPhotoPaths = saveToTempFile(compliedPhotos);

        //publish message to RabbitMQ
        RabbitMQPhotoUploadMessage message = new RabbitMQPhotoUploadMessage(
                observationSaved.getObservationId(),
                inspectionPhotoPaths,
                compliedPhotoPaths
        );
        amqpTemplate.convertAndSend(
                RabbitMQConfig.PHOTO_UPLOAD_EXCHANGE,
                RabbitMQConfig.PHOTO_UPLOAD_ROUTING_KEY,
                message
        );
        log.info("Photo upload message published for inspectionId={}", observationSaved.getObservationId());

        return createResponse(observationSaved);
    }

    private List<byte[]> convertToBytes(List<MultipartFile> files){
        if(files==null || files.isEmpty()){
            return Collections.emptyList();
        }

        return  files.stream().map(file -> {
            try{
                return file.getBytes();
            }catch (IOException e){
                throw new RuntimeException("Failed to read file bytes", e);
            }
        }).toList();
    }

    private List<String> saveToTempFile(List<MultipartFile> files){
        if(files==null || files.isEmpty()){
            return Collections.emptyList();
        }

        return files.stream().map(file->{
          try{
              Path tempFile = Files.createTempFile("upload","_"+file.getOriginalFilename());
              file.transferTo(tempFile.toFile());
              return tempFile.toAbsolutePath().toString();
          } catch (IOException e) {
              throw new RuntimeException("Unable to create temp file", e);
          }
        }).toList();
    }

    InspectionObservationResponse createResponse(InspectionObservationEntity observation){
        InspectionObservationResponse response = new InspectionObservationResponse();
        response.setObservationId(observation.getObservationId());
        response.setUserId(observation.getUserId());
        response.setInspectionDate(observation.getInspectionDate());
        response.setCategory(observation.getCategory());
        response.setDepartment(observation.getDepartment());
        response.setSubDepartment(observation.getSubDepartment());
        response.setLocation(observation.getLocation());
        response.setObservation(observation.getObservation());
        response.setComplianceStatus(observation.getComplianceStatus());
        response.setTargetDate(observation.getTargetDate());
        response.setObservationPhotoUrl(observation.getInspectionPhotoUrl());
        response.setCompliedPhotoUrl(observation.getCompliedPhotoUrl());
        response.setPhotoUploadStatus(observation.getPhotoUploadStatus());
        response.setCreatedAt(observation.getCreatedAt());
        response.setUpdatedAt(observation.getUpdatedAt());

        return response;
    }
}
