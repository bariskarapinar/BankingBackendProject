package com.fintech.wallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDTO {
    private String errorCode;
    private String message;
    private String details;
    private Instant timestamp;
    private String path;
    private int status;
    private List<String> validationErrors;
}
