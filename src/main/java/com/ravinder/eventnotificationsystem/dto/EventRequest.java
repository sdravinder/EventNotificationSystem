package com.ravinder.eventnotificationsystem.dto;


import com.ravinder.eventnotificationsystem.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for event creation requests via REST API.
 * This class represents the request body for POST /api/events endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotNull(message = "Event type is required")
    private EventType eventType;

    /**
     * Event-specific payload data
     * The structure depends on the event type:
     * - EMAIL: { "recipient": "email", "message": "text", "subject": "optional" }
     * - SMS: { "phoneNumber": "+1234567890", "message": "text", "senderId": "optional" }
     * - PUSH: { "deviceId": "device-id", "message": "text", "title": "optional" }
     */
    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    @NotBlank(message = "Callback URL is required")
    @Pattern(regexp = "^https?://.*", message = "Callback URL must be a valid HTTP/HTTPS URL")
    private String callbackUrl;
}
