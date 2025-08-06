package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import com.ravinder.eventnotificationsystem.service.CallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for event processors following Template Method pattern.
 * This class implements common processing logic while allowing subclasses to
 * define specific processing behavior for different event types.
 * <p>
 * SOLID Principles:
 * - Single Responsibility: Handles common processing logic
 * - Open/Closed: Open for extension through abstract methods
 * - Liskov Substitution: All concrete processors can substitute this base
 * - Template Method Pattern: Defines the algorithm structure
 */
@Slf4j
@ManagedResource(objectName = "com.ravinder.eventnotificationsystem:type=EventProcessor")
public abstract class EventProcessor<T extends Event> implements Runnable {

    protected final EventQueueManager queueManager;
    protected final CallbackService callbackService;
    protected final EventType eventType;
    protected final int processingDelaySeconds;
    protected final int failureRatePercent;
    protected final Random random;

    // Thread control
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private Thread processingThread;

    // Metrics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);

    /**
     * Constructor for EventProcessor
     *
     * @param queueManager           the queue manager for dequeuing events
     * @param callbackService        the callback service for sending notifications
     * @param eventType              the type of events this processor handles
     * @param processingDelaySeconds the delay in seconds for processing each event
     * @param failureRatePercent     the percentage of events that should fail randomly (0-100)
     */
    protected EventProcessor(EventQueueManager queueManager,
                             CallbackService callbackService,
                             EventType eventType,
                             int processingDelaySeconds,
                             int failureRatePercent) {
        this.queueManager = queueManager;
        this.callbackService = callbackService;
        this.eventType = eventType;
        this.processingDelaySeconds = processingDelaySeconds;
        this.failureRatePercent = failureRatePercent;
        this.random = new Random();

        validateConstructorParameters();
    }

    /**
     * Validate constructor parameters
     */
    private void validateConstructorParameters() {
        if (queueManager == null) {
            throw new IllegalArgumentException("Queue manager cannot be null");
        }
        if (callbackService == null) {
            throw new IllegalArgumentException("Callback service cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (processingDelaySeconds < 0) {
            throw new IllegalArgumentException("Processing delay cannot be negative");
        }
        if (failureRatePercent < 0 || failureRatePercent > 100) {
            throw new IllegalArgumentException("Failure rate must be between 0 and 100");
        }
    }

    /**
     * Start the processor thread
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            processingThread = new Thread(this, getThreadName());
            processingThread.start();
            log.info("Started {} processor thread", eventType);
        } else {
            log.warn("Processor for {} is already running", eventType);
        }
    }

    /**
     * Stop the processor thread gracefully
     */
    public void stop() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("Shutting down {} processor", eventType);
            if (processingThread != null) {
                processingThread.interrupt();
            }
        }
    }

    /**
     * Main processing loop - Template Method pattern
     */
    @Override
    public void run() {
        log.info("Started processing loop for {} events", eventType);

        while (running.get() && !shutdown.get()) {
            try {
                // Step 1: Dequeue event (blocking operation)
                Event event = queueManager.dequeue(eventType);

                // Step 2: Cast to specific event type for type safety
                @SuppressWarnings("unchecked")
                T typedEvent = (T) event;

                // Step 3: Process the event using template method
                processEvent(typedEvent);

            } catch (InterruptedException e) {
                log.info("Processor for {} was interrupted", eventType);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in {} processor", eventType, e);
            }
        }

        running.set(false);
        log.info("Stopped processing loop for {} events", eventType);
    }

    /**
     * Process a single event - Template Method implementation
     *
     * @param event the event to process
     */
    private void processEvent(T event) {
        try {
            log.debug("Processing event {} of type {}", event.getEventId(), eventType);

            // Mark event as processing
            event.markAsProcessing();

            // Simulate processing time
            simulateProcessingDelay();

            // Check for random failure
            if (shouldSimulateFailure()) {
                handleProcessingFailure(event, "Simulated random failure (10% rate)");
                return;
            }

            // Validate event before processing
            if (!event.isValidPayload()) {
                handleProcessingFailure(event, "Invalid event payload");
                return;
            }

            // Process the specific event type (abstract method)
            processSpecificEvent(event);

            // Mark as completed
            event.markAsCompleted();
            successCount.incrementAndGet();

            // Send callback notification
            callbackService.sendCallbackAsync(event);

            log.info("Successfully processed event {} of type {}",
                    event.getEventId(), eventType);

        } catch (Exception e) {
            handleProcessingFailure(event, "Processing error: " + e.getMessage());
            log.error("Failed to process event {} of type {}",
                    event.getEventId(), eventType, e);
        } finally {
            processedCount.incrementAndGet();
        }
    }

    /**
     * Handle processing failure by marking event as failed
     *
     * @param event        the failed event
     * @param errorMessage the error message
     */
    private void handleProcessingFailure(T event, String errorMessage) {
        event.markAsFailed(errorMessage);
        failedCount.incrementAndGet();
        
        // Send callback notification
        callbackService.sendCallbackAsync(event);
        
        log.warn("Event {} failed: {}", event.getEventId(), errorMessage);
    }

    /**
     * Simulate processing delay based on event type
     */
    private void simulateProcessingDelay() {
        try {
            Thread.sleep(processingDelaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    /**
     * Determine if this event should fail randomly based on configured failure rate
     *
     * @return true if the event should fail
     */
    private boolean shouldSimulateFailure() {
        if (failureRatePercent == 0) {
            return false;
        }

        // Generate random number between 0-99, fail if < failureRatePercent
        int randomValue = random.nextInt(100);
        return randomValue < failureRatePercent;
    }

    /**
     * Abstract method for processing specific event types.
     * processors must implement this method to define
     * event-type-specific processing logic.
     *
     * @param event the event to process
     * @throws Exception if processing fails
     */
    protected abstract void processSpecificEvent(T event) throws Exception;

    /**
     * Get the thread name for this processor
     *
     * @return the thread name
     */
    protected String getThreadName() {
        return eventType.name().toLowerCase() + "-processor";
    }

    // Getters for monitoring and testing

    public EventType getEventType() {
        return eventType;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    // JMX Managed Attributes for monitoring

    @ManagedAttribute(description = "Total number of events processed")
    public long getProcessedCount() {
        return processedCount.get();
    }

    @ManagedAttribute(description = "Number of events that failed processing")
    public long getFailedCount() {
        return failedCount.get();
    }

    @ManagedAttribute(description = "Number of events processed successfully")
    public long getSuccessCount() {
        return successCount.get();
    }

    @ManagedAttribute(description = "Success rate as percentage")
    public double getSuccessRate() {
        long total = processedCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount.get() / total * 100.0;
    }

    @ManagedAttribute(description = "Failure rate as percentage")
    public double getFailureRate() {
        long total = processedCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failedCount.get() / total * 100.0;
    }

    @ManagedAttribute(description = "Whether the processor is currently running")
    public boolean getIsRunning() {
        return isRunning();
    }

    @ManagedAttribute(description = "Processing delay in seconds")
    public int getProcessingDelaySeconds() {
        return processingDelaySeconds;
    }
}
