package com.ravinder.eventnotificationsystem.controller;

import com.ravinder.eventnotificationsystem.dto.EventRequest;
import com.ravinder.eventnotificationsystem.dto.EventResponse;
import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.model.EventFactory;
import com.ravinder.eventnotificationsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Event Notification System.
 * 
 * This controller provides endpoints for:
 * - Creating and submitting events for processing
 * - Event ID generation and response management
 * 
 * Supports three event types: EMAIL, SMS, and PUSH notifications.
 *
 */
@RestController
@RequestMapping("/api/events")
@Slf4j
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Submit a new event for processing.
     * 
     * @param eventRequest the event request containing type, payload, and callback URL
     * @return EventResponse with generated event ID
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest eventRequest) {
        
        log.info("Received event creation request: type={}, callbackUrl={}", 
                 eventRequest.getEventType(), eventRequest.getCallbackUrl());
        
        // Create event from request (includes event ID generation)
        Event event = EventFactory.createEvent(eventRequest);
        
        // Submit event for processing
        eventService.submitEvent(event);
        
        log.info("Event created successfully: eventId={}, type={}", 
                 event.getEventId(), event.getEventType());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EventResponse.success(event.getEventId()));
    }
}
