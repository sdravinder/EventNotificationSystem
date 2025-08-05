package com.ravinder.eventnotificationsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for event creation responses from the REST API.
 * This class represents the response body for successful POST /api/events requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private String eventId;
    private String message;

    /**
     * Factory method to create a successful event response
     */
    public static EventResponse success(String eventId) {
        return new EventResponse(eventId, "Event accepted for processing.");
    }

    /**
     * Factory method to create a successful event response with custom message
     */
    public static EventResponse success(String eventId, String message) {
        return new EventResponse(eventId, message);
    }
}
