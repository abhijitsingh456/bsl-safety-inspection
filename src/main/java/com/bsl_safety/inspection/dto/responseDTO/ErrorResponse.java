package com.bsl_safety.inspection.dto.responseDTO;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {

    private Instant time;
    private int status;
    private String error;
    private String message;
    private String path;
}