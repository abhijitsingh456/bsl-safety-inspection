package com.bsl_safety.inspection.service.impl;

import com.bsl_safety.inspection.configuration.RabbitMQConfig;
import com.bsl_safety.inspection.dto.requestDTO.InspectionObservationRequest;
import com.bsl_safety.inspection.dto.requestDTO.RabbitMQPhotoUploadMessage;
import com.bsl_safety.inspection.dto.responseDTO.InspectionObservationResponse;
import com.bsl_safety.inspection.entity.InspectionObservationEntity;
import com.bsl_safety.inspection.exception.DataIntegrityException;
import com.bsl_safety.inspection.repository.InspectionObservationRepository;
import com.bsl_safety.inspection.service.CloudinaryUploadService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InspectionObservationServiceImplTest {

    @Mock
    private InspectionObservationRepository inspectionObservationRepository;

    @Mock
    private CloudinaryUploadService cloudinaryUploadService;

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private InspectionObservationServiceImpl service;

    private InspectionObservationRequest request;
    private InspectionObservationEntity savedEntity;

    @BeforeEach
    void setUp(){

        request = new InspectionObservationRequest();
        request.setInspectionDate(LocalDate.of(2025, 5,1));
        request.setDepartment("BF");
        request.setLocation("CH-4");
        request.setObservation("Dust accumulation");
        request.setCategory("General");
        request.setSubDepartment("BF-3");
        request.setComplianceStatus("Pending");
        request.setTargetDate(LocalDate.of(2025,5,10));

        savedEntity = new InspectionObservationEntity();
        savedEntity.setObservationId(UUID.randomUUID());
        savedEntity.setUserId(UUID.randomUUID());
        savedEntity.setInspectionDate(request.getInspectionDate());
        savedEntity.setDepartment(request.getDepartment());
        savedEntity.setLocation(request.getLocation());
        savedEntity.setObservation(request.getObservation());
        savedEntity.setCategory(request.getCategory());
        savedEntity.setSubDepartment(request.getSubDepartment());
        savedEntity.setComplianceStatus(request.getComplianceStatus());
        savedEntity.setTargetDate(request.getTargetDate());
        savedEntity.setPhotoUploadStatus("PENDING");
        savedEntity.setIsDeleted(Boolean.FALSE);

    }

    //=================================================================//
    //===========DUPLICATE DETECTION TEST==============================//
    //=================================================================//

    @Test
    @DisplayName("Should throw DataIntegrityException when duplicate observation exists")
    void shouldThrowExceptionWhenDuplicateObservationExists(){
        //Arrange
        //tells fake repository to say "yes this hash already exists"
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(true);

        //Act & Assert
        assertThatThrownBy(()->
                service.createInspectionObservation(request, null, null)
        )
                .isInstanceOf(DataIntegrityException.class)
                .hasMessage("Observation already exists");

        //also verify that repository never tried to save anything
        verify(inspectionObservationRepository, never()).save(any());
    }

    //=================================================================//
    //===============HASH GENERATION TEST==============================//
    //=================================================================//

    @Test
    @DisplayName("Should generate hash from date, department, location, observation")
    void shouldGenerateCorrectHash(){
        //Arrange
        //We capture what hash value gets passed to existsByObservationHash
        //so we can verify it was computed correctly
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any())).thenReturn(savedEntity);

        //Compute the expected hash
        String expectedRaw = String.format("%s|%s|%s|%s",
                request.getInspectionDate().toString(),
                request.getDepartment().trim().toLowerCase(),
                request.getLocation().trim().toLowerCase(),
                request.getObservation().trim().toLowerCase());
        String expectedHash = DigestUtils.sha256Hex(expectedRaw);

        //Act
        service.createInspectionObservation(request, null, null);

        //Assert - capture the actual hash passed to repository and compare
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(inspectionObservationRepository).existsByObservationHash(hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isEqualTo(expectedHash);

    }

    //=================================================================//
    //===============ENTITY MAPPING TEST==============================//
    //=================================================================//
    @Test
    @DisplayName("Should map all request fields to entity correctly")
    void shouldMapRequestFieldsToEntityCorrectly(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //ArgumentCaptor captures the actual entity passed to the repository.save()
        //so we can inspect all its field values
        ArgumentCaptor<InspectionObservationEntity> entityCaptor = ArgumentCaptor.forClass(InspectionObservationEntity.class);

        //Act
        service.createInspectionObservation(request, null, null);

        verify(inspectionObservationRepository).save(entityCaptor.capture());
        InspectionObservationEntity capturedEntity = entityCaptor.getValue();

        assertThat(capturedEntity.getInspectionDate()).isEqualTo(request.getInspectionDate());
        assertThat(capturedEntity.getCategory()).isEqualTo(request.getCategory());
        assertThat(capturedEntity.getDepartment()).isEqualTo(request.getDepartment());
        assertThat(capturedEntity.getSubDepartment()).isEqualTo(request.getSubDepartment());
        assertThat(capturedEntity.getLocation()).isEqualTo(request.getLocation());
        assertThat(capturedEntity.getObservation()).isEqualTo(request.getObservation());
        assertThat(capturedEntity.getComplianceStatus()).isEqualTo(request.getComplianceStatus());
        assertThat(capturedEntity.getTargetDate()).isEqualTo(request.getTargetDate());
    }


    @Test
    @DisplayName("Should set photoUploadStatus to PNEDING_UPLOAD on new entity")
    void shouldSetPhotoUploadStatusToPendingUpload(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //ArgumentCaptor captures the actual entity passed to the repository.save()
        //so we can inspect all its field values
        ArgumentCaptor<InspectionObservationEntity> entityCaptor = ArgumentCaptor.forClass(InspectionObservationEntity.class);

        //Act
        service.createInspectionObservation(request, null, null);

        verify(inspectionObservationRepository).save(entityCaptor.capture());
        InspectionObservationEntity capturedEntity = entityCaptor.getValue();

        assertThat(capturedEntity.getPhotoUploadStatus()).isEqualTo("PENDING_UPLOAD");
    }

    @Test
    @DisplayName("Should set isDeleted to False on new entity")
    void shouldSetIsDeletedToFalse(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //ArgumentCaptor captures the actual entity passed to the repository.save()
        //so we can inspect all its field values
        ArgumentCaptor<InspectionObservationEntity> entityCaptor = ArgumentCaptor.forClass(InspectionObservationEntity.class);

        //Act
        service.createInspectionObservation(request, null, null);

        verify(inspectionObservationRepository).save(entityCaptor.capture());
        InspectionObservationEntity capturedEntity = entityCaptor.getValue();

        assertThat(capturedEntity.getIsDeleted()).isFalse();
    }

    //=================================================================//
    //===============RABBITMQ MESSAGE TEST=============================//
    //=================================================================//

    @Test
    @DisplayName("Should publish message to correct RabbitMQ exchange & routing key")
    void shouldPublishMessageToCorrectExchangeAndRoutingKey(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //Act
        service.createInspectionObservation(request, null, null);

        //Assert - verify the exact exchange and routing key used
        verify(amqpTemplate).convertAndSend(
                eq(RabbitMQConfig.PHOTO_UPLOAD_EXCHANGE),
                eq(RabbitMQConfig.PHOTO_UPLOAD_ROUTING_KEY),
                any(RabbitMQPhotoUploadMessage.class)
        );
    }

    @Test
    @DisplayName("Should publish message with correct observationId")
    void shouldPublishMessageWithCorrectObservationId(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);


        ArgumentCaptor<RabbitMQPhotoUploadMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQPhotoUploadMessage.class);

        //Act
        service.createInspectionObservation(request, null, null);

        //Assert - capture the actual message and check its observationId
        verify(amqpTemplate).convertAndSend(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().getInspectionId())
                .isEqualTo(savedEntity.getObservationId());
    }

    @Test
    @DisplayName("Should publish photo with empty photo paths when no photo provided")
    void shouldPublishEmptyPhotoPathsWhenNoPhotoProvided(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);


        ArgumentCaptor<RabbitMQPhotoUploadMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQPhotoUploadMessage.class);

        //Act - pass null for both photo lists
        service.createInspectionObservation(request, null, null);

        verify(amqpTemplate).convertAndSend(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().getInspectionPhotoPaths().size()).isEqualTo(0);
        assertThat(messageCaptor.getValue().getCompliedPhotoPaths().size()).isEqualTo(0);

    }

    //=================================================================//
    //=======================TEMP FILE TEST============================//
    //=================================================================//

    @Test
    @DisplayName("Should create temp file and include paths in RabbitMQ message")
    void shouldCreateTempFileAndIncludeInRabbitMqMessage(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //MockMultipartFile simulates a real file upload
        List<MultipartFile> inspectionPhotos = List.of(
                new MockMultipartFile(
                        "photo1",
                        "inspection1.jpg",
                        "image/jpeg",
                        "fake-image-bytes".getBytes()
                )
        );

        ArgumentCaptor<RabbitMQPhotoUploadMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQPhotoUploadMessage.class);

        //Act
        service.createInspectionObservation(request, inspectionPhotos, null);

        //Assert - the message should have one temp file path
        verify(amqpTemplate).convertAndSend(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        List<String> paths = messageCaptor.getValue().getInspectionPhotoPaths();

        assertThat(paths.size()).isEqualTo(1);
        assertThat(paths.get(0)).contains("upload"); //temp file prefix
        assertThat(paths.get(0)).contains("inspection1.jpg"); //original file name

    }

    @Test
    @DisplayName("Should return empty path list when inspection photo list is null")
    void shouldReturnEmptyPathsWhenPhotoListIsNull(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        ArgumentCaptor<RabbitMQPhotoUploadMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQPhotoUploadMessage.class);

        //Act
        service.createInspectionObservation(request, null, null);

        //Assert - the message should have one temp file path
        verify(amqpTemplate).convertAndSend(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        List<String> inspectionPhotoPaths = messageCaptor.getValue().getInspectionPhotoPaths();
        List<String> compliedPhotoPaths = messageCaptor.getValue().getCompliedPhotoPaths();

        assertThat(inspectionPhotoPaths.size()).isEqualTo(0);
        assertThat(compliedPhotoPaths.size()).isEqualTo(0);

    }

    @Test
    @DisplayName("Should return empty path list when inspection photo list is empty")
    void shouldReturnEmptyPathsWhenPhotoListIsEmpty(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        ArgumentCaptor<RabbitMQPhotoUploadMessage> messageCaptor =
                ArgumentCaptor.forClass(RabbitMQPhotoUploadMessage.class);

        //Act
        service.createInspectionObservation(request, List.of(), null);

        //Assert - the message should have one temp file path
        verify(amqpTemplate).convertAndSend(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        List<String> inspectionPhotoPaths = messageCaptor.getValue().getInspectionPhotoPaths();

        assertThat(inspectionPhotoPaths.size()).isEqualTo(0);

    }

    //=================================================================//
    //================RESPONSE MAPPING TEST============================//
    //=================================================================//

    @Test
    @DisplayName("Should return response mapped from saved entity")
    void shouldReturnResponseMappedFromSavedEntity(){
        //Arrange
        when(inspectionObservationRepository.existsByObservationHash(anyString()))
                .thenReturn(false);
        when(inspectionObservationRepository.save(any()))
                .thenReturn(savedEntity);

        //Act
        InspectionObservationResponse response =
                service.createInspectionObservation(request, List.of(), null);

        //Assert - response should reflect what the repository returned
        assertThat(response).isNotNull();
        assertThat(response.getObservationId()).isEqualTo(savedEntity.getObservationId());
        assertThat(response.getDepartment()).isEqualTo(savedEntity.getDepartment());
        assertThat(response.getLocation()).isEqualTo(savedEntity.getLocation());
        assertThat(response.getPhotoUploadStatus()).isEqualTo(savedEntity.getPhotoUploadStatus());

    }
}
