package com.ravinder.eventnotificationsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for error responses from the REST API.
 * This class represents the response body for failed requests with validation errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {


    private int status;
    private String error;
    private String message;
    private List<String> validationErrors;
    private LocalDateTime timestamp;
    private String path;

    /**
     * Constructor for simple error responses
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for validation error responses
     */
    public ErrorResponse(int status, String error, String message,
                         List<String> validationErrors, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.validationErrors = validationErrors;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}
