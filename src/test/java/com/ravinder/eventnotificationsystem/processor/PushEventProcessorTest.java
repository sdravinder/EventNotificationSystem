package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EventStatus;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.model.PushEvent;
import com.ravinder.eventnotificationsystem.model.PushPayload;
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
 * Unit tests for PushEventProcessor
 */
@ExtendWith(MockitoExtension.class)
class PushEventProcessorTest {

    @Mock
    private EventQueueManager queueManager;

    @Mock
    private EventNotificationProperties properties;

    @Mock
    private EventNotificationProperties.Processing processingProperties;

    @Mock
    private EventNotificationProperties.Processing.Push pushProcessingProperties;

    @Mock
    private CallbackService callbackService;

    private PushEventProcessor processor;
    private BlockingQueue<PushEvent> pushQueue;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Setup mock properties
        when(properties.getProcessing()).thenReturn(processingProperties);
        when(processingProperties.getPush()).thenReturn(pushProcessingProperties);
        when(processingProperties.getFailureRatePercent()).thenReturn(0);
        when(pushProcessingProperties.getDelaySeconds()).thenReturn(0); // No delay for testing

        pushQueue = new LinkedBlockingQueue<>();
        lenient().when(queueManager.dequeue(EventType.PUSH)).thenAnswer(invocation -> pushQueue.take());

        processor = new PushEventProcessor(queueManager, callbackService, properties);
    }

    @Test
    void testProcessorInitialization() {
        assertThat(processor.getEventType()).isEqualTo(EventType.PUSH);
        assertThat(processor.getProcessingDelaySeconds()).isEqualTo(0);
        assertThat(processor.isRunning()).isFalse();
    }

    @Test
    void testSuccessfulPushProcessing() throws InterruptedException {
        // Create valid push event
        PushPayload payload = new PushPayload(
                "device123", "Hello World!");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-1");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushProcessingWithLongMessage() throws InterruptedException {
        // Create push with maximum length message
        String longMessage = "A".repeat(500); // Max allowed length
        PushPayload payload = new PushPayload(
                "device123", longMessage);
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-long");

        // Add to queue and start processing
        pushQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify successful processing
        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(processor.getSuccessCount()).isEqualTo(1);
    }

    @Test
    void testInvalidPushPayload() throws InterruptedException {
        // Create push with null payload
        PushEvent event = new PushEvent("http://callback.url", null);
        event.setEventId("push-null-payload");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithEmptyDeviceId() throws InterruptedException {
        // Create push with empty device ID
        PushPayload payload = new PushPayload(
                "", "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-empty-device");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithNullDeviceId() throws InterruptedException {
        // Create push with null device ID
        PushPayload payload = new PushPayload(
                null, "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-null-device");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithShortDeviceId() throws InterruptedException {
        // Create push with device ID too short (less than 3 characters)
        PushPayload payload = new PushPayload(
                "ab", "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-short-device");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithLongDeviceId() throws InterruptedException {
        // Create push with device ID too long (more than 255 characters)
        String longDeviceId = "A".repeat(256);
        PushPayload payload = new PushPayload(
                longDeviceId, "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-long-device");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithEmptyMessage() throws InterruptedException {
        // Create push with empty message
        PushPayload payload = new PushPayload(
                "device123", "");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-empty-message");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithNullMessage() throws InterruptedException {
        // Create push with null message
        PushPayload payload = new PushPayload(
                "device123", null);
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-null-message");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushWithTooLongMessage() throws InterruptedException {
        // Create push with message exceeding limit
        String tooLongMessage = "A".repeat(501); // Exceeds 500 char limit
        PushPayload payload = new PushPayload(
                "device123", tooLongMessage);
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-too-long");

        // Add to queue and start processing
        pushQueue.offer(event);
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
    void testPushServiceFailureSimulation() throws InterruptedException {
        // Create push with "invalid" in device ID to trigger service failure
        PushPayload payload = new PushPayload(
                "invalid-device", "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-service-failure");

        // Add to queue and start processing
        pushQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing due to simulated service failure
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid device ID rejected by push notification service");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testPushServiceFailureWithZeroDeviceId() throws InterruptedException {
        // Create push with "000" device ID to trigger service failure
        PushPayload payload = new PushPayload(
                "000", "Test message");
        PushEvent event = new PushEvent("http://callback.url", payload);
        event.setEventId("push-zero-device");

        // Add to queue and start processing
        pushQueue.offer(event);
        processor.start();

        // Wait for processing
        Thread.sleep(100);
        processor.stop();

        // Verify failed processing due to simulated service failure
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Invalid device ID rejected by push notification service");
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testMultiplePushProcessing() throws InterruptedException {
        // Create multiple push events
        for (int i = 1; i <= 3; i++) {
            PushPayload payload = new PushPayload(
                    "device" + i, "Message " + i);
            PushEvent event = new PushEvent("http://callback.url", payload);
            event.setEventId("push-" + i);
            pushQueue.offer(event);
        }

        // Start processing
        processor.start();

        // Wait for all events to be processed
        Thread.sleep(300);
        processor.stop();

        // Verify all pushes were processed successfully
        assertThat(processor.getProcessedCount()).isEqualTo(3);
        assertThat(processor.getSuccessCount()).isEqualTo(3);
        assertThat(processor.getFailedCount()).isEqualTo(0);
        assertThat(processor.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    void testThreadName() {
        assertThat(processor.getThreadName()).isEqualTo("push-processor-thread");
    }

    @Test
    void testValidDeviceIdBoundaries() throws InterruptedException {
        // Test minimum valid device ID length (3 characters)
        PushPayload payload1 = new PushPayload(
                "abc", "Test message");
        PushEvent event1 = new PushEvent("http://callback.url", payload1);
        event1.setEventId("push-min-device");

        pushQueue.offer(event1);
        processor.start();
        Thread.sleep(100);
        processor.stop();

        assertThat(event1.getStatus()).isEqualTo(EventStatus.COMPLETED);

        // Reset for next test
        processor = new PushEventProcessor(queueManager, callbackService, properties);
        pushQueue.clear();

        // Test maximum valid device ID length (255 characters)
        String maxDeviceId = "A".repeat(255);
        PushPayload payload2 = new PushPayload(
                maxDeviceId, "Test message");
        PushEvent event2 = new PushEvent("http://callback.url", payload2);
        event2.setEventId("push-max-device");

        pushQueue.offer(event2);
        processor.start();
        Thread.sleep(100);
        processor.stop();

        assertThat(event2.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    void testValidMessageBoundaries() throws InterruptedException {
        // Test minimum valid message length (1 character)
        PushPayload payload1 = new PushPayload(
                "device123", "A");
        PushEvent event1 = new PushEvent("http://callback.url", payload1);
        event1.setEventId("push-min-message");

        pushQueue.offer(event1);
        processor.start();
        Thread.sleep(100);
        processor.stop();

        assertThat(event1.getStatus()).isEqualTo(EventStatus.COMPLETED);

        // Reset for next test
        processor = new PushEventProcessor(queueManager, callbackService, properties);
        pushQueue.clear();

        // Test maximum valid message length (500 characters)
        String maxMessage = "A".repeat(500);
        PushPayload payload2 = new PushPayload(
                "device123", maxMessage);
        PushEvent event2 = new PushEvent("http://callback.url", payload2);
        event2.setEventId("push-max-message");

        pushQueue.offer(event2);
        processor.start();
        Thread.sleep(100);
        processor.stop();

        assertThat(event2.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }
}
