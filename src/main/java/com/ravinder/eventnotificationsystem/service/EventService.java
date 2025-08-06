package com.ravinder.eventnotificationsystem.service;

import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service layer for handling event operations.
 * 
 * This service provides business logic for:
 * - Event submission and queuing
 * - Event validation
 * - Integration with queue management system
 */
@Service
@Slf4j
public class EventService {

    private final EventQueueManager queueManager;
    private final CallbackService callbackService;

    @Autowired
    public EventService(EventQueueManager queueManager, CallbackService callbackService) {
        this.queueManager = queueManager;
        this.callbackService = callbackService;
    }

    /**
     * Submit an event for processing.
     * 
     * @param event the event to be processed
     * @throws IllegalArgumentException if event validation fails
     * @throws RuntimeException if event submission fails
     */
    public void submitEvent(Event event) {
        log.debug("Submitting event for processing: eventId={}, type={}", 
                  event.getEventId(), event.getEventType());
        
        // Validate event payload
        if (!event.isValidPayload()) {
            String message = "Invalid event payload for event type: " + event.getEventType();
            log.error("Event validation failed: eventId={}, message={}", 
                      event.getEventId(), message);
            throw new IllegalArgumentException(message);
        }
        
        try {
            // Add event to appropriate queue
            boolean queued = queueManager.enqueue(event);
            
            if (!queued) {
                String message = "Failed to queue event - queue may be full";
                log.error("Event queuing failed: eventId={}, type={}", 
                          event.getEventId(), event.getEventType());
                throw new RuntimeException(message);
            }
            
            log.info("Event successfully queued: eventId={}, type={}", 
                     event.getEventId(), event.getEventType());
            
        } catch (Exception e) {
            log.error("Failed to submit event: eventId={}, type={}", 
                      event.getEventId(), event.getEventType(), e);
            throw new RuntimeException("Failed to submit event for processing", e);
        }
    }

    /**
     * Get the callback service for external access.
     * 
     * @return the callback service instance
     */
    public CallbackService getCallbackService() {
        return callbackService;
    }
}
