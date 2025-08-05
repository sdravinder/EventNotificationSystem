package com.ravinder.eventnotificationsystem.model;

/**
 * Enumeration representing the different types of events supported by the system.
 * Each event type has its own processing characteristics and queue.
 */
public enum EventType {
    /**
     * Email notification event - processed with 5 second delay
     */
    EMAIL,
    
    /**
     * SMS notification event - processed with 3 second delay
     */
    SMS,
    
    /**
     * Push notification event - processed with 2 second delay
     */
    PUSH
}
