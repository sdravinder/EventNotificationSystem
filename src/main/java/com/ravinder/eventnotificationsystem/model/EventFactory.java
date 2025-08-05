package com.ravinder.eventnotificationsystem.model;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravinder.eventnotificationsystem.dto.EventRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating Event instances from EventRequest DTOs.
 * Handles the conversion of generic payload maps to specific event types.
 */
@Slf4j
public class EventFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create an Event instance from an EventRequest DTO
     *
     * @param request the event request containing type, payload, and callback URL
     * @return the created Event instance
     * @throws IllegalArgumentException if the request is invalid or payload cannot be parsed
     */
    public static Event createEvent(EventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Event request cannot be null");
        }

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required");
        }

        if (request.getPayload() == null) {
            throw new IllegalArgumentException("Event payload is required");
        }

        if (request.getCallbackUrl() == null || request.getCallbackUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Callback URL is required");
        }

        Event event = createEventByType(request.getEventType(), request.getPayload(), request.getCallbackUrl());
        event.setEventId(generateEventId());

        // Validate the created event
        if (!event.isValidPayload()) {
            throw new IllegalArgumentException("Invalid payload for event type: " + request.getEventType());
        }

        log.debug("Created event: {} with ID: {}", event.getEventType(), event.getEventId());
        return event;
    }

    /**
     * Create specific event type based on the event type enum
     *
     * @param eventType   the Type of the Event
     * @param payloadMap  Request payload received in the request
     * @param callbackUrl the callbackURL to post the status to
     * @return specific event based on the eventType
     */
    private static Event createEventByType(EventType eventType, Map<String, Object> payloadMap, String callbackUrl) {
        try {
            return switch (eventType) {
                case EMAIL -> {
                    EmailPayload emailPayload = objectMapper.convertValue(payloadMap, EmailPayload.class);
                    yield new EmailEvent(callbackUrl, emailPayload);
                }
                case SMS -> {
                    SmsPayload smsPayload = objectMapper.convertValue(payloadMap, SmsPayload.class);
                    yield new SmsEvent(callbackUrl, smsPayload);
                }
                case PUSH -> {
                    PushPayload pushPayload = objectMapper.convertValue(payloadMap, PushPayload.class);
                    yield new PushEvent(callbackUrl, pushPayload);
                }
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
            };
        } catch (Exception e) {
            log.error("Failed to create event of type {} from payload: {}", eventType, payloadMap, e);
            throw new IllegalArgumentException("Failed to parse payload for event type: " + eventType + ". " + e.getMessage());
        }
    }

    /**
     * Generate a unique event ID
     */
    private static String generateEventId() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
