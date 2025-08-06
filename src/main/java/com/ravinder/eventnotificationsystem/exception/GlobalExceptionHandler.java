package com.ravinder.eventnotificationsystem.exception;

import com.ravinder.eventnotificationsystem.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.util.List;

/**
 * Global exception handler for the Event Notification System.
 * 
 * This class handles all exceptions thrown by controllers and provides
 * consistent error responses across the application.
 * 
 * Features:
 * - Validation error handling
 * - Business logic exception handling  
 * - Generic exception handling
 * - Proper HTTP status codes
 * - Structured error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException e, WebRequest webRequest) {
        
        BindingResult bindingResult = e.getBindingResult();
        List<String> validationErrors = bindingResult.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Invalid request data",
                validationErrors,
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        log.warn("Validation failed: {}", validationErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions (business logic validation)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, WebRequest webRequest) {
        
        log.error("Invalid argument: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Payload",
                e.getMessage(),
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle HTTP method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, WebRequest webRequest) {
        
        log.warn("Method not supported: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                "HTTP method not supported for this endpoint",
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle unsupported media type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e, WebRequest webRequest) {
        
        log.warn("Unsupported media type: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Unsupported Media Type",
                "Content type not supported. Please use application/json",
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handle runtime exceptions (service layer failures)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException e, WebRequest webRequest) {
        
        log.error("Runtime exception occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Failed to process request",
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception e, WebRequest webRequest) {
        
        log.error("Unhandled exception occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                webRequest.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
