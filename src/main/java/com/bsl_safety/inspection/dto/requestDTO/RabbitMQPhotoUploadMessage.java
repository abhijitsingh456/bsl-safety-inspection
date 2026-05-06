package com.bsl_safety.inspection.dto.requestDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RabbitMQPhotoUploadMessage implements Serializable {

    private UUID inspectionId;
    private List<String> inspectionPhotoPaths;
    private List<String> compliedPhotoPaths;

}
