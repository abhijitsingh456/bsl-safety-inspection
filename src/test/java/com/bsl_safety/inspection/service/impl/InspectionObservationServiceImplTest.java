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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
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

    private InspectionObservationEntity buildEntity(UUID id) {
        InspectionObservationEntity entity = new InspectionObservationEntity();
        entity.setObservationId(id);
        entity.setUserId(UUID.randomUUID());
        entity.setInspectionDate(LocalDate.of(2024, 1, 15));
        entity.setCategory("Safety");
        entity.setDepartment("Engineering");
        entity.setSubDepartment("Mechanical");
        entity.setLocation("Plant A");
        entity.setObservation("Fire extinguisher missing");
        entity.setComplianceStatus("NON_COMPLIANT");
        entity.setTargetDate(LocalDate.of(2024, 2, 15));
        entity.setToBeIncludedInDispatcher("YES");
        entity.setRecommendations("Replace immediately");
        entity.setDiscussedWith("Site Manager");
        entity.setInspectionPhotoUrl(List.of("https://cloudinary.com/photo1.jpg"));
        entity.setCompliedPhotoUrl(List.of("https://cloudinary.com/complied1.jpg"));
        entity.setPhotoUploadStatus("PENDING_UPLOAD");
        entity.setIsDeleted(Boolean.FALSE);
        entity.setObservationHash("dummy-hash-123");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private Page<InspectionObservationEntity> buildPage(
            List<InspectionObservationEntity> entities,
            Pageable pageable,
            long totalElements) {
        return new PageImpl<>(entities, pageable, totalElements);
    }

    @BeforeEach
    void setUp(){

        ReflectionTestUtils.setField(service, "uploadDir", "uploads");

        request = new InspectionObservationRequest();
        request.setInspectionDate(LocalDate.of(2025, 5,1));
        request.setDepartment("BF");
        request.setLocation("CH-4");
        request.setObservation("Dust accumulation");
        request.setCategory("General");
        request.setSubDepartment("BF-3");
        request.setComplianceStatus("Pending");
        request.setToBeIncludedInDispatcher("YES");
        request.setRecommendations("Dust to be cleaned");
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
        savedEntity.setToBeIncludedInDispatcher(request.getToBeIncludedInDispatcher());
        savedEntity.setRecommendations(request.getRecommendations());
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
        assertThat(response.getToBeIncludedInDispatcher()).isEqualTo(savedEntity.getToBeIncludedInDispatcher());
        assertThat(response.getRecommendations()).isEqualTo(savedEntity.getRecommendations());
        assertThat(response.getPhotoUploadStatus()).isEqualTo(savedEntity.getPhotoUploadStatus());

    }

    // -----------------------------------------------------------------------
    // findInspectionObservation() TESTS
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return paginated results when all filters are provided")
    void shouldReturnPaginatedResultsWhenAllFiltersProvided(){

        //Arrange
        Pageable pageable = PageRequest.of(0,25);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<InspectionObservationEntity> entityList = List.of(buildEntity(id1), buildEntity(id2));
        Page<InspectionObservationEntity> entityPage = buildPage(entityList, pageable, 2);

        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(entityPage);

        //Act
        Page<InspectionObservationResponse> result = service.findInspectionObservation(
                List.of("Engineering"),
                List.of("Safety"),
                List.of("NON_COMPLIANT"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 28),
                null,
                pageable
        );

        // Assert
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should normalize empty department list to null")
    void shouldNormalizeEmptyDepartmentToNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(), pageable, 0));

        ArgumentCaptor<List<String>> departmentCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        service.findInspectionObservation(
                List.of(),  // empty → should become null
                null, null, null, null, null, null, null, pageable
        );

        // Assert
        verify(inspectionObservationRepository).searchObservations(
                departmentCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any()
        );
        assertThat(departmentCaptor.getValue()).isNull();
    }


    @Test
    @DisplayName("Should normalize empty category list to null")
    void shouldNormalizeEmptyCategoryToNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(), pageable, 0));

        ArgumentCaptor<List<String>> categoryCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        service.findInspectionObservation(
                null,
                List.of(),  // empty → should become null
                null, null, null, null, null, null, pageable
        );

        // Assert
        verify(inspectionObservationRepository).searchObservations(
                any(),
                categoryCaptor.capture(),
                any(), any(), any(), any(), any(), any()
        );
        assertThat(categoryCaptor.getValue()).isNull();
    }


    @Test
    @DisplayName("Should normalize empty complianceStatus list to null")
    void shouldNormalizeEmptyComplianceStatusToNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(), pageable, 0));

        ArgumentCaptor<List<String>> complianceCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        service.findInspectionObservation(
                null, null,
                List.of(),  // empty → should become null
                null, null, null, null, null, pageable
        );

        // Assert
        verify(inspectionObservationRepository).searchObservations(
                any(), any(),
                complianceCaptor.capture(),
                any(), any(), any(), any(), any()
        );
        assertThat(complianceCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("Should pass date parameters directly to repository")
    void shouldPassDateParametersToRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(), pageable, 0));

        LocalDate inspectionStart = LocalDate.of(2024, 1, 1);
        LocalDate inspectionEnd   = LocalDate.of(2024, 1, 31);
        LocalDate targetStart     = LocalDate.of(2024, 2, 1);
        LocalDate targetEnd       = LocalDate.of(2024, 2, 28);

        ArgumentCaptor<LocalDate> inspectionStartCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> inspectionEndCaptor   = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> targetStartCaptor     = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> targetEndCaptor       = ArgumentCaptor.forClass(LocalDate.class);

        // Act
        service.findInspectionObservation(
                null, null, null,
                inspectionStart, inspectionEnd,
                targetStart, targetEnd,
                null, pageable
        );

        // Assert
        verify(inspectionObservationRepository).searchObservations(
                any(), any(), any(),
                inspectionStartCaptor.capture(),
                inspectionEndCaptor.capture(),
                targetStartCaptor.capture(),
                targetEndCaptor.capture(),
                any()
        );
        assertThat(inspectionStartCaptor.getValue()).isEqualTo(inspectionStart);
        assertThat(inspectionEndCaptor.getValue()).isEqualTo(inspectionEnd);
        assertThat(targetStartCaptor.getValue()).isEqualTo(targetStart);
        assertThat(targetEndCaptor.getValue()).isEqualTo(targetEnd);
    }


    @Test
    @DisplayName("Should map entity fields to response correctly")
    void shouldMapEntityFieldsToResponseCorrectly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        InspectionObservationEntity entity = buildEntity(UUID.randomUUID());
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(entity), pageable, 1));

        // Act
        Page<InspectionObservationResponse> result = service.findInspectionObservation(
                null, null, null, null, null, null, null, null, pageable
        );

        // Assert
        InspectionObservationResponse response = result.getContent().get(0);
        assertThat(response.getObservationId()).isEqualTo(entity.getObservationId());
        assertThat(response.getDepartment()).isEqualTo(entity.getDepartment());
        assertThat(response.getCategory()).isEqualTo(entity.getCategory());
        assertThat(response.getComplianceStatus()).isEqualTo(entity.getComplianceStatus());
    }


    @Test
    @DisplayName("Should return empty page when repository returns no results")
    void shouldReturnEmptyPageWhenNoResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(buildPage(List.of(), pageable, 0));

        // Act
        Page<InspectionObservationResponse> result = service.findInspectionObservation(
                null, null, null, null, null, null, null, null, pageable
        );

        // Assert
        assertThat(result.getContent().size()).isEqualTo(0);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }


    @Test
    @DisplayName("Should return correct page metadata for multi-page results")
    void shouldReturnCorrectPageMetadata() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);
        List<InspectionObservationEntity> entities = List.of(
                buildEntity(UUID.randomUUID()), buildEntity(UUID.randomUUID()), buildEntity(UUID.randomUUID()),
                buildEntity(UUID.randomUUID()), buildEntity(UUID.randomUUID())
        );
        // 5 results on this page, 23 total across all pages
        Page<InspectionObservationEntity> entityPage = buildPage(entities, pageable, 23);

        when(inspectionObservationRepository.searchObservations(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(entityPage);

        // Act
        Page<InspectionObservationResponse> result = service.findInspectionObservation(
                null, null, null, null, null, null, null, null, pageable
        );

        // Assert
        assertThat(result.getContent().size()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(23);
        assertThat(result.getTotalPages()).isEqualTo(5);  // ceil(23/5)
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

}
