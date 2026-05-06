package com.bsl_safety.inspection.service;

import com.bsl_safety.inspection.dto.requestDTO.RabbitMQPhotoUploadMessage;

import java.io.IOException;

public interface RabbitMQPhotoUploadConsumer {
    void handlePhotoUpload(RabbitMQPhotoUploadMessage message) throws IOException;
}
