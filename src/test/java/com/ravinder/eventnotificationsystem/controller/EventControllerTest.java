package com.ravinder.eventnotificationsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravinder.eventnotificationsystem.dto.EventRequest;
import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EventController
 */
@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateEvent_ValidEmailEvent_Success() throws Exception {
        // Prepare test data
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test email message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        request.setPayload(payload);
        request.setCallbackUrl("http://callback.example.com");

        // Mock service
        doNothing().when(eventService).submitEvent(any(Event.class));

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.message", is("Event accepted for processing.")));

        verify(eventService, times(1)).submitEvent(any(Event.class));
    }

    @Test
    void testCreateEvent_ValidSmsEvent_Success() throws Exception {
        // Prepare test data
        Map<String, Object> payload = new HashMap<>();
        payload.put("phoneNumber", "+1234567890");
        payload.put("message", "Test SMS message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.SMS);
        request.setPayload(payload);
        request.setCallbackUrl("https://callback.example.com");

        // Mock service
        doNothing().when(eventService).submitEvent(any(Event.class));

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.message", is("Event accepted for processing.")));

        verify(eventService, times(1)).submitEvent(any(Event.class));
    }

    @Test
    void testCreateEvent_ValidPushEvent_Success() throws Exception {
        // Prepare test data
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", "device123");
        payload.put("message", "Test push notification");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.PUSH);
        request.setPayload(payload);
        request.setCallbackUrl("http://callback.example.com");

        // Mock service
        doNothing().when(eventService).submitEvent(any(Event.class));

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.message", is("Event accepted for processing.")));

        verify(eventService, times(1)).submitEvent(any(Event.class));
    }
}
