package com.fintech.wallet.api.exception;

import com.fintech.wallet.api.dto.ErrorResponseDTO;
import com.fintech.wallet.common.exception.ApplicationException;
import com.fintech.wallet.common.exception.DomainException;
import com.fintech.wallet.common.exception.InfrastructureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDTO> handleDomainException(
            DomainException ex,
            WebRequest request) {

        log.error("Domain exception: {}", ex.getErrorCode(), ex);

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleApplicationException(
            ApplicationException ex,
            WebRequest request) {

        log.error("Application exception: {}", ex.getErrorCode(), ex);

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.CONFLICT.value())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponseDTO> handleInfrastructureException(
            InfrastructureException ex,
            WebRequest request) {

        log.error("Infrastructure exception: {}", ex.getErrorCode(), ex);

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation exception", ex);

        BindingResult bindingResult = ex.getBindingResult();
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.add(error.getField() + ": " + error.getDefaultMessage())
        );

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed")
                .details("Check validationErrors for details")
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected exception", ex);

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .details(ex.getMessage())
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
