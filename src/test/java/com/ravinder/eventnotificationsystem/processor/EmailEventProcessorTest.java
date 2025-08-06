package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EmailEvent;
import com.ravinder.eventnotificationsystem.model.EmailPayload;
import com.ravinder.eventnotificationsystem.model.EventStatus;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import com.ravinder.eventnotificationsystem.service.CallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailEventProcessor
 */
@ExtendWith(MockitoExtension.class)
class EmailEventProcessorTest {

    @Mock
    private EventQueueManager queueManager;

    @Mock
    private CallbackService callbackService;

    @Mock
    private EventNotificationProperties properties;

    @Mock
    private EventNotificationProperties.Processing processingProperties;

    @Mock
    private EventNotificationProperties.Processing.Email emailProcessingProperties;

    private EmailEventProcessor processor;
    private BlockingQueue<EmailEvent> emailQueue;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Setup mock properties
        when(properties.getProcessing()).thenReturn(processingProperties);
        when(processingProperties.getEmail()).thenReturn(emailProcessingProperties);
        when(processingProperties.getFailureRatePercent()).thenReturn(0);
        when(emailProcessingProperties.getDelaySeconds()).thenReturn(0); // No delay for testing

        emailQueue = new LinkedBlockingQueue<>();
        lenient().when(queueManager.dequeue(EventType.EMAIL)).thenAnswer(invocation -> emailQueue.take());

        processor = new EmailEventProcessor(queueManager, callbackService, properties);
    }

    @Test
    void testProcessorInitialization() {
        assertThat(processor.getEventType()).isEqualTo(EventType.EMAIL);
        assertThat(processor.getProcessingDelaySeconds()).isEqualTo(0);
        assertThat(processor.isRunning()).isFalse();
    }

    @Test
    void testSuccessfulEmailProcessing() throws InterruptedException {
        // Create valid email event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Hello World");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-1");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify successful processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(processor.getProcessedCount()).isEqualTo(1);
        assertThat(processor.getSuccessCount()).isEqualTo(1);
        assertThat(processor.getFailedCount()).isEqualTo(0);
    }

    @Test
    void testEmailProcessingWithLongMessage() throws InterruptedException {
        // Create email with maximum length message
        String longMessage = "A".repeat(1000); // Max allowed length
        EmailPayload payload = new EmailPayload(
                "test@example.com", longMessage);
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-long");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify successful processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(processor.getSuccessCount()).isEqualTo(1);
    }

    @Test
    void testInvalidEmailPayload() throws InterruptedException {
        // Create email with null payload
        EmailEvent event = new EmailEvent("http://callback.url", null);
        event.setEventId("email-null-payload");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("Invalid event payload");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testEmailWithEmptyRecipient() throws InterruptedException {
        // Create email with empty recipient
        EmailPayload payload = new EmailPayload(
                "", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-empty-recipient");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid event payload");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testEmailWithInvalidFormat() throws InterruptedException {
        // Create email with invalid format
        EmailPayload payload = new EmailPayload(
                "invalid-email-format", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-invalid-format");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid event payload");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testEmailWithEmptyMessage() throws InterruptedException {
        // Create email with empty message
        EmailPayload payload = new EmailPayload(
                "test@example.com", "");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-empty-message");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid event payload");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testEmailWithTooLongMessage() throws InterruptedException {
        // Create email with message exceeding limit
        String tooLongMessage = "A".repeat(1001); // Exceeds 1000 char limit
        EmailPayload payload = new EmailPayload(
                "test@example.com", tooLongMessage);
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-too-long");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid event payload");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testEmailServiceFailureSimulation() throws InterruptedException {
        // Create email with "invalid" in recipient to trigger service failure
        EmailPayload payload = new EmailPayload(
                "invalid@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("email-service-failure");

        // Add to queue and start processing
        emailQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing due to simulated service failure
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid email address rejected by email service");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testMultipleEmailProcessing() throws InterruptedException {
        // Create multiple email events
        for (int i = 1; i <= 3; i++) {
            EmailPayload payload = new EmailPayload(
                    "user" + i + "@example.com", "Message " + i);
            EmailEvent event = new EmailEvent("http://callback.url", payload);
            event.setEventId("email-" + i);
            emailQueue.offer(event);
        }

        // Start processing
        processor.start();

        // Wait for all events to be processed
        Thread.sleep(300);
        processor.stop();

        // Verify all emails were processed successfully
        assertThat(processor.getProcessedCount()).isEqualTo(3);
        assertThat(processor.getSuccessCount()).isEqualTo(3);
        assertThat(processor.getFailedCount()).isEqualTo(0);
        assertThat(processor.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    void testThreadName() {
        assertThat(processor.getThreadName()).isEqualTo("email-processor-thread");
    }
}
