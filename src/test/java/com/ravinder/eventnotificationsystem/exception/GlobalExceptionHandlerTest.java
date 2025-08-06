package com.ravinder.eventnotificationsystem.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravinder.eventnotificationsystem.controller.EventController;
import com.ravinder.eventnotificationsystem.dto.EventRequest;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GlobalExceptionHandler
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private EventService eventService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        EventController eventController = new EventController(eventService);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testHandleValidationErrors_MissingEventType() throws Exception {
        // Prepare test data with missing event type
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest();
        // eventType is null
        request.setPayload(payload);
        request.setCallbackUrl("http://callback.example.com");

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.message", is("Invalid request data")))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.path", notNullValue()));

        verify(eventService, never()).submitEvent(any());
    }

    @Test
    void testHandleValidationErrors_MissingPayload() throws Exception {
        // Prepare test data with missing payload
        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        // payload is null
        request.setCallbackUrl("http://callback.example.com");

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)));

        verify(eventService, never()).submitEvent(any());
    }

    @Test
    void testHandleValidationErrors_InvalidCallbackUrl() throws Exception {
        // Prepare test data with invalid callback URL
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        request.setPayload(payload);
        request.setCallbackUrl("invalid-url"); // Invalid URL format

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)));

        verify(eventService, never()).submitEvent(any());
    }

    @Test
    void testHandleValidationErrors_EmptyCallbackUrl() throws Exception {
        // Prepare test data with empty callback URL
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        request.setPayload(payload);
        request.setCallbackUrl(""); // Empty URL

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));

        verify(eventService, never()).submitEvent(any());
    }


    @Test
    void testHandleRuntimeException() throws Exception {
        // Prepare test data
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        request.setPayload(payload);
        request.setCallbackUrl("http://callback.example.com");

        // Mock service to throw RuntimeException
        doThrow(new RuntimeException("Database connection failed"))
                .when(eventService).submitEvent(any());

        // Execute and verify
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Failed to process request")));

        verify(eventService, times(1)).submitEvent(any());
    }

    @Test
    void testHandleMethodNotSupported() throws Exception {
        // Execute GET request on POST endpoint
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")))
                .andExpect(jsonPath("$.message", is("HTTP method not supported for this endpoint")));

        verify(eventService, never()).submitEvent(any());
    }

    @Test
    void testHandleUnsupportedMediaType() throws Exception {
        // Prepare test data
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest();
        request.setEventType(EventType.EMAIL);
        request.setPayload(payload);
        request.setCallbackUrl("http://callback.example.com");

        // Execute without Content-Type header
        mockMvc.perform(post("/api/events")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.error", is("Unsupported Media Type")))
                .andExpect(jsonPath("$.message", is("Content type not supported. Please use application/json")));

        verify(eventService, never()).submitEvent(any());
    }

}
