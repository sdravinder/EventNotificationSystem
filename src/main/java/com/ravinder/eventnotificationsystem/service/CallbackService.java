package com.ravinder.eventnotificationsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravinder.eventnotificationsystem.dto.CallbackRequest;
import com.ravinder.eventnotificationsystem.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling callback notifications to external systems.
 * 
 * This service provides:
 * - HTTP client for callback notifications
 * - Retry mechanism for failed callbacks (up to 3 attempts)
 * - Asynchronous callback processing
 */
@Service
@Slf4j
public class CallbackService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService callbackExecutor;
    
    @Value("${callback.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${callback.retry.delayMs:1000}")
    private long retryDelayMs;
    
    @Value("${callback.timeout.connectionMs:5000}")
    private int connectionTimeoutMs;
    
    @Value("${callback.timeout.readMs:10000}")
    private int readTimeoutMs;

    @Autowired
    public CallbackService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.callbackExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread thread = new Thread(r, "callback-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Send callback notification asynchronously with retry mechanism.
     * 
     * @param event the event to send callback for
     */
    public void sendCallbackAsync(Event event) {
        if (event.getCallbackUrl() == null || event.getCallbackUrl().trim().isEmpty()) {
            log.debug("No callback URL provided for event: {}", event.getEventId());
            return;
        }
        
        CompletableFuture.runAsync(() -> sendCallbackWithRetry(event), callbackExecutor)
                .exceptionally(throwable -> {
                    log.error("Unexpected error in callback processing for event: {}", 
                              event.getEventId(), throwable);
                    return null;
                });
    }

    /**
     * Send callback notification with retry mechanism.
     * 
     * @param event the event to send callback for
     */
    private void sendCallbackWithRetry(Event event) {
        CallbackRequest callbackRequest = createCallbackRequest(event);
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                log.debug("Sending callback for event: {} (attempt {}/{})", 
                          event.getEventId(), attempt, maxRetryAttempts);
                
                sendCallback(event.getCallbackUrl(), callbackRequest);
                
                log.info("Callback sent successfully for event: {} (attempt {})", 
                         event.getEventId(), attempt);
                return;
                
            } catch (Exception e) {
                log.warn("Callback attempt {} failed for event: {} - {}", 
                         attempt, event.getEventId(), e.getMessage());
                
                if (attempt == maxRetryAttempts) {
                    log.error("All callback attempts failed for event: {}", event.getEventId(), e);
                } else {
                    // Wait before retry
                    try {
                        Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Callback retry interrupted for event: {}", event.getEventId());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Send HTTP callback request.
     * 
     * @param callbackUrl the URL to send callback to
     * @param callbackRequest the callback payload
     * @throws Exception if callback fails
     */
    private void sendCallback(String callbackUrl, CallbackRequest callbackRequest) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "EventNotificationSystem/1.0");
        
        HttpEntity<CallbackRequest> requestEntity = new HttpEntity<>(callbackRequest, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    callbackUrl, 
                    requestEntity, 
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Callback successful: status={}, eventId={}", 
                          response.getStatusCode(), callbackRequest.getEventId());
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Client error during callback: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new RuntimeException("Server error during callback: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Network error during callback: " + e.getMessage(), e);
        }
    }

    /**
     * Create callback request payload from event.
     * 
     * @param event the event to create callback for
     * @return callback request object
     */
    private CallbackRequest createCallbackRequest(Event event) {
        return CallbackRequest.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .processedAt(event.getProcessedAt())
                .errorMessage(event.getErrorMessage())
                .build();
    }

    /**
     * Shutdown the callback executor gracefully.
     */
    public void shutdown() {
        log.info("Shutting down callback service");
        callbackExecutor.shutdown();
        try {
            if (!callbackExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                callbackExecutor.shutdownNow();
                if (!callbackExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Callback executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            callbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
