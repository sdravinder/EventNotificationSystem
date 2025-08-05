package com.ravinder.eventnotificationsystem.model;


public enum EventStatus {
    /**
     * Event has been received and queued for processing
     */
    PENDING,
    
    /**
     * Event is currently being processed
     */
    PROCESSING,
    
    /**
     * Event has been successfully processed and completed
     */
    COMPLETED,
    
    /**
     * Event processing failed due to an error
     */
    FAILED
}
