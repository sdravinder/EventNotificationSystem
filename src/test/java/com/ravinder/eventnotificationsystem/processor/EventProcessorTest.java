package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.model.EmailEvent;
import com.ravinder.eventnotificationsystem.model.EmailPayload;
import com.ravinder.eventnotificationsystem.model.Event;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventProcessor abstract class and its template method pattern
 */
@ExtendWith(MockitoExtension.class)
class EventProcessorTest {

    @Mock
    private EventQueueManager queueManager;

    @Mock
    private CallbackService callbackService;

    private TestEventProcessor processor;
    private BlockingQueue<Event> testQueue;

    /**
     * Concrete implementation for testing the abstract EventProcessor
     */
    private static class TestEventProcessor extends EventProcessor<EmailEvent> {
        private boolean shouldThrowException = false;
        private int processCallCount = 0;

        public TestEventProcessor(EventQueueManager queueManager,
                                  CallbackService callbackService,
                                  int processingDelaySeconds,
                                  int failureRatePercent) {
            super(queueManager, callbackService, EventType.EMAIL, processingDelaySeconds, failureRatePercent);
        }

        @Override
        protected void processSpecificEvent(EmailEvent event) throws Exception {
            processCallCount++;
            if (shouldThrowException) {
                throw new RuntimeException("Test processing exception");
            }
        }

        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }

        public int getProcessCallCount() {
            return processCallCount;
        }
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        testQueue = new LinkedBlockingQueue<>();
        lenient().when(queueManager.dequeue(EventType.EMAIL)).thenAnswer(invocation -> testQueue.take());

        processor = new TestEventProcessor(queueManager, callbackService, 0, 0); // No delay, no random failures for testing
    }

    @Test
    void testConstructorValidation() {
        // Test null queue manager
        assertThatThrownBy(() -> new TestEventProcessor(null, callbackService, 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Queue manager cannot be null");

        // Test negative processing delay
        assertThatThrownBy(() -> new TestEventProcessor(queueManager, callbackService, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Processing delay cannot be negative");

        // Test invalid failure rate
        assertThatThrownBy(() -> new TestEventProcessor(queueManager, callbackService, 5, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failure rate must be between 0 and 100");

        assertThatThrownBy(() -> new TestEventProcessor(queueManager, callbackService, 5, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failure rate must be between 0 and 100");
    }

    @Test
    void testProcessorInitialState() {
        assertThat(processor.getEventType()).isEqualTo(EventType.EMAIL);
        assertThat(processor.isRunning()).isFalse();
        assertThat(processor.isShutdown()).isFalse();
        assertThat(processor.getProcessedCount()).isEqualTo(0);
        assertThat(processor.getFailedCount()).isEqualTo(0);
        assertThat(processor.getSuccessCount()).isEqualTo(0);
        assertThat(processor.getSuccessRate()).isEqualTo(0.0);
        assertThat(processor.getFailureRate()).isEqualTo(0.0);
    }

    @Test
    void testStartAndStop() throws InterruptedException {
        // Start processor
        processor.start();

        // Wait a bit for thread to start
        Thread.sleep(50);
        assertThat(processor.isRunning()).isTrue();
        assertThat(processor.isShutdown()).isFalse();

        // Stop processor
        processor.stop();

        // Wait for shutdown
        Thread.sleep(100);
        assertThat(processor.isShutdown()).isTrue();
    }

    @Test
    void testSuccessfulEventProcessing() throws InterruptedException {
        // Create test event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-event-1");

        // Add event to queue
        testQueue.offer(event);

        // Start processor
        processor.start();

        // Wait for processing
        Thread.sleep(100);

        // Stop processor
        processor.stop();

        // Verify event was processed successfully
        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(processor.getProcessCallCount()).isEqualTo(1);
        assertThat(processor.getProcessedCount()).isEqualTo(1);
        assertThat(processor.getSuccessCount()).isEqualTo(1);
        assertThat(processor.getFailedCount()).isEqualTo(0);
        assertThat(processor.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    void testFailedEventProcessing() throws InterruptedException {
        // Configure processor to throw exception
        processor.setShouldThrowException(true);

        // Create test event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-event-1");

        // Add event to queue
        testQueue.offer(event);

        // Start processor
        processor.start();

        // Wait for processing
        Thread.sleep(100);

        // Stop processor
        processor.stop();

        // Verify event failed
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).contains("Processing error");
        assertThat(processor.getProcessCallCount()).isEqualTo(1);
        assertThat(processor.getProcessedCount()).isEqualTo(1);
        assertThat(processor.getSuccessCount()).isEqualTo(0);
        assertThat(processor.getFailedCount()).isEqualTo(1);
        assertThat(processor.getFailureRate()).isEqualTo(100.0);
    }

    @Test
    void testInvalidEventPayload() throws InterruptedException {
        // Create event with invalid payload
        EmailEvent event = new EmailEvent("http://callback.url", null);
        event.setEventId("test-event-1");

        // Add event to queue
        testQueue.offer(event);

        // Start processor
        processor.start();

        // Wait for processing
        Thread.sleep(100);

        // Stop processor
        processor.stop();

        // Verify event failed due to invalid payload
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("Invalid event payload");
        assertThat(processor.getProcessCallCount()).isEqualTo(0); // Should not reach processSpecificEvent
        assertThat(processor.getProcessedCount()).isEqualTo(1);
        assertThat(processor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testRandomFailureSimulation() throws InterruptedException {
        // Create processor with 100% failure rate
        TestEventProcessor failureProcessor = new TestEventProcessor(queueManager, callbackService, 0, 100);

        // Create test event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-event-1");

        // Add event to queue
        testQueue.offer(event);

        // Start processor
        failureProcessor.start();

        // Wait for processing
        Thread.sleep(100);

        // Stop processor
        failureProcessor.stop();

        // Verify event failed due to simulated failure
        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("Simulated random failure (10% rate)");
        assertThat(failureProcessor.getProcessCallCount()).isEqualTo(0); // Should not reach processSpecificEvent
        assertThat(failureProcessor.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testProcessingDelay() throws InterruptedException {
        // Create processor with 1 second delay
        TestEventProcessor delayProcessor = new TestEventProcessor(queueManager, callbackService, 1, 0);

        // Create test event
        EmailPayload payload = new EmailPayload(
                "test@example.com", "Test message");
        EmailEvent event = new EmailEvent("http://callback.url", payload);
        event.setEventId("test-event-1");

        // Add event to queue
        testQueue.offer(event);

        // Start processor and measure time
        long startTime = System.currentTimeMillis();
        delayProcessor.start();

        // Wait for processing to complete
        Thread.sleep(1200); // Wait a bit longer than the delay

        delayProcessor.stop();
        long endTime = System.currentTimeMillis();

        // Verify processing took at least the specified delay
        long processingTime = endTime - startTime;
        assertThat(processingTime).isGreaterThanOrEqualTo(1000);
        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    void testMultipleEventProcessing() throws InterruptedException {
        // Create multiple events
        for (int i = 1; i <= 5; i++) {
            EmailPayload payload = new EmailPayload(
                    "test" + i + "@example.com", "Test message " + i);
            EmailEvent event = new EmailEvent("http://callback.url", payload);
            event.setEventId("test-event-" + i);
            testQueue.offer(event);
        }

        // Start processor
        processor.start();

        // Wait for all events to be processed
        Thread.sleep(500);

        // Stop processor
        processor.stop();

        // Verify all events were processed
        assertThat(processor.getProcessCallCount()).isEqualTo(5);
        assertThat(processor.getProcessedCount()).isEqualTo(5);
        assertThat(processor.getSuccessCount()).isEqualTo(5);
        assertThat(processor.getFailedCount()).isEqualTo(0);
        assertThat(processor.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    void testThreadNameGeneration() {
        assertThat(processor.getThreadName()).isEqualTo("email-processor");
    }

    @Test
    void testDoubleStart() throws InterruptedException {
        processor.start();
        Thread.sleep(50);

        // Try to start again - should not create another thread
        processor.start();

        // Should still be running
        assertThat(processor.isRunning()).isTrue();

        processor.stop();
    }
}
