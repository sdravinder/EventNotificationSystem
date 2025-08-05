package com.ravinder.eventnotificationsystem.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.ravinder.eventnotificationsystem.model.EventStatus;
import com.ravinder.eventnotificationsystem.model.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for callback notifications sent to client systems.
 * This class represents the request body sent to the callback URL
 * when event processing is completed or failed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallbackRequest {


    private String eventId;
    /**
     * Final status of the event processing (COMPLETED or FAILED)
     */
    private EventStatus status;

    /**
     * Type of the processed event ( EMAIL / SMS / PUSH )
     */
    private EventType eventType;
    private LocalDateTime processedAt;
    private String errorMessage;

    /**
     * Factory method to create a successful callback request
     */
    public static CallbackRequest success(String eventId, EventType eventType, LocalDateTime processedAt) {
        return CallbackRequest.builder()
                .eventId(eventId)
                .status(EventStatus.COMPLETED)
                .eventType(eventType)
                .processedAt(processedAt)
                .build();
    }

    /**
     * Factory method to create a failed callback request
     */
    public static CallbackRequest failure(String eventId, EventType eventType,
                                          LocalDateTime processedAt, String errorMessage) {
        return CallbackRequest.builder()
                .eventId(eventId)
                .status(EventStatus.FAILED)
                .eventType(eventType)
                .processedAt(processedAt)
                .errorMessage(errorMessage)
                .build();
    }
}
