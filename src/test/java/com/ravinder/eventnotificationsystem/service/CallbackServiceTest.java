package com.ravinder.eventnotificationsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravinder.eventnotificationsystem.dto.CallbackRequest;
import com.ravinder.eventnotificationsystem.model.EmailEvent;
import com.ravinder.eventnotificationsystem.model.EmailPayload;
import com.ravinder.eventnotificationsystem.model.EventStatus;
import com.ravinder.eventnotificationsystem.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CallbackService
 */
@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CallbackService callbackService;

    private EmailEvent testEvent;

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(callbackService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(callbackService, "retryDelayMs", 100L);

        // Create test event
        EmailPayload payload = new EmailPayload("test@example.com", "Test message");
        testEvent = new EmailEvent("http://callback.example.com", payload);
        testEvent.setEventId("test-event-123");
        testEvent.markAsCompleted();
    }

    @Test
    void testSendCallbackAsync_SuccessfulCallback() throws Exception {
        // Mock successful REST call
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait a bit for async execution
        Thread.sleep(200);

        // Verify
        verify(restTemplate, times(1)).postForEntity(
                eq("http://callback.example.com"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testSendCallbackAsync_NoCallbackUrl() throws Exception {
        // Create event without callback URL
        testEvent.setCallbackUrl(null);

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait a bit for async execution
        Thread.sleep(200);

        // Verify no REST call was made
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSendCallbackAsync_EmptyCallbackUrl() throws Exception {
        // Create event with empty callback URL
        testEvent.setCallbackUrl("");

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait a bit for async execution
        Thread.sleep(200);

        // Verify no REST call was made
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSendCallbackAsync_RetryOnFailure() throws Exception {
        // Mock first two calls to fail, third to succeed
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait for async execution and retries
        Thread.sleep(1000);

        // Verify 3 attempts were made
        verify(restTemplate, times(3)).postForEntity(
                eq("http://callback.example.com"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testSendCallbackAsync_MaxRetriesExceeded() throws Exception {
        // Mock all calls to fail
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait for async execution and retries
        Thread.sleep(1000);

        // Verify 3 attempts were made (max retries)
        verify(restTemplate, times(3)).postForEntity(
                eq("http://callback.example.com"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testSendCallbackAsync_ClientError() throws Exception {
        // Mock client error response
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Execute
        callbackService.sendCallbackAsync(testEvent);

        // Wait for async execution and retries
        Thread.sleep(1000);

        // Verify 3 attempts were made (retries even on client errors)
        verify(restTemplate, times(3)).postForEntity(
                eq("http://callback.example.com"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testCreateCallbackRequest_CompletedEvent() {
        // Test completed event
        testEvent.markAsCompleted();
        testEvent.setProcessedAt(LocalDateTime.now());

        // Use reflection to call private method
        CallbackRequest result = (CallbackRequest) ReflectionTestUtils.invokeMethod(
                callbackService, "createCallbackRequest", testEvent);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("test-event-123");
        assertThat(result.getEventType()).isEqualTo(EventType.EMAIL);
        assertThat(result.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void testCreateCallbackRequest_FailedEvent() {
        // Test failed event
        testEvent.markAsFailed("Processing failed");

        // Use reflection to call private method
        CallbackRequest result = (CallbackRequest) ReflectionTestUtils.invokeMethod(
                callbackService, "createCallbackRequest", testEvent);

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo("test-event-123");
        assertThat(result.getEventType()).isEqualTo(EventType.EMAIL);
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Processing failed");
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void testSendCallback_SuccessfulRequest() throws Exception {
        // Create callback request
        CallbackRequest callbackRequest = CallbackRequest.builder()
                .eventId("test-event-123")
                .eventType(EventType.EMAIL)
                .status(EventStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();

        // Mock successful response
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        // Use reflection to call private method
        ReflectionTestUtils.invokeMethod(callbackService, "sendCallback", 
                "http://callback.example.com", callbackRequest);

        // Verify
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<CallbackRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1)).postForEntity(
                eq("http://callback.example.com"),
                entityCaptor.capture(),
                eq(String.class)
        );

        HttpEntity<CallbackRequest> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getBody()).isEqualTo(callbackRequest);
        assertThat(capturedEntity.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
    }

    @Test
    void testSendCallback_UnexpectedStatusCode() throws Exception {
        // Mock unexpected status response
        ResponseEntity<String> unexpectedResponse = new ResponseEntity<>("UnAccepted", HttpStatus.NOT_FOUND);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(unexpectedResponse);

        // This should throw an exception when called through sendCallbackWithRetry
        testEvent.setCallbackUrl("http://callback.example.com");
        callbackService.sendCallbackAsync(testEvent);

        // Wait for async execution
        Thread.sleep(500);

        // Verify retries were attempted
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testShutdown() {
        // Test shutdown method
        callbackService.shutdown();
        
        // Verify that the method completes without exceptions
        // (ExecutorService shutdown is tested indirectly)
    }
}
