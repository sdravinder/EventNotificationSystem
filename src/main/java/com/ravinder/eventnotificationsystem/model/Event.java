package com.ravinder.eventnotificationsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Base class for all event types in the notification system.
 * This abstract class contains common properties shared by all events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Event {

    private String eventId;

    /**
     * Type of the event (EMAIL, SMS, PUSH)
     */
    private EventType eventType;

    /**
     * Current status of the event processing ( PENDING / PROCESSING / COMPLETED / FAILED)
     */
    private EventStatus status;

    private String callbackUrl;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String errorMessage;

    private Map<String, Object> metadata;

    public Event(EventType eventType, String callbackUrl) {
        this.eventType = eventType;
        this.callbackUrl = callbackUrl;
        this.status = EventStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Abstract method to get the event payload.
     * Each concrete event type must implement this method.
     *
     * @return the payload specific to the event type
     */
    public abstract Object getPayload();

    /**
     * Abstract method to validate the event payload.
     * Each concrete event type must implement this method.
     *
     * @return true if the payload is valid, false otherwise
     */
    public abstract boolean isValidPayload();

    /**
     * Mark the event as processing and set the processed timestamp
     */
    public void markAsProcessing() {
        this.status = EventStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark the event as completed
     */
    public void markAsCompleted() {
        this.status = EventStatus.COMPLETED;
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }

    /**
     * Mark the event as failed with an error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }
}
