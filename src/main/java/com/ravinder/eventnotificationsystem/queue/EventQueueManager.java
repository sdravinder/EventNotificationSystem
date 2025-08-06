package com.ravinder.eventnotificationsystem.queue;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.Event;
import com.ravinder.eventnotificationsystem.model.EventType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages separate BlockingQueues for each event type to ensure FIFO processing.
 * <p>
 * Features:
 * - Thread-safe queue operations using LinkedBlockingQueue
 * - Separate queue per event type (EMAIL, SMS, PUSH)
 * - JMX monitoring for queue sizes and metrics
 * - Configurable queue capacities
 */
@Component
@Slf4j
@ManagedResource(objectName = "com.ravinder.eventnotificationsystem:type=EventQueueManager")
public class EventQueueManager {

    private final EventNotificationProperties properties;
    private final Map<EventType, BlockingQueue<Event>> queues;

    @Autowired
    public EventQueueManager(EventNotificationProperties properties) {
        this.properties = properties;
        this.queues = new EnumMap<>(EventType.class);
    }

    /**
     * Initialize queues with configured capacities after bean construction
     */
    @PostConstruct
    public void initializeQueues() {
        // Initialize EMAIL queue
        int emailCapacity = properties.getQueue().getEmail().getCapacity();
        queues.put(EventType.EMAIL, new LinkedBlockingQueue<>(emailCapacity));

        // Initialize SMS queue
        int smsCapacity = properties.getQueue().getSms().getCapacity();
        queues.put(EventType.SMS, new LinkedBlockingQueue<>(smsCapacity));

        // Initialize PUSH queue
        int pushCapacity = properties.getQueue().getPush().getCapacity();
        queues.put(EventType.PUSH, new LinkedBlockingQueue<>(pushCapacity));

        log.info("Initialized queues - EMAIL: {}, SMS: {}, PUSH: {}",
                emailCapacity, smsCapacity, pushCapacity);
    }

    /**
     * Add an event to the appropriate queue based on its type
     *
     * @param event the event to queue
     * @return true if the event was successfully queued, false if queue is full
     * @throws IllegalArgumentException if event or event type is null
     */
    public boolean enqueue(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getEventType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        BlockingQueue<Event> queue = getQueue(event.getEventType());
        boolean success = queue.offer(event);

        if (success) {
            log.debug("Event {} queued successfully in {} queue",
                    event.getEventId(), event.getEventType());
        } else {
            log.warn("Failed to queue event {} - {} queue is full",
                    event.getEventId(), event.getEventType());
        }

        return success;
    }

    /**
     * Retrieve and remove an event from the specified queue type.
     * This method blocks until an event is available.
     *
     * @param eventType the type of queue to dequeue from
     * @return the next event from the queue
     * @throws InterruptedException     if the thread is interrupted while waiting
     * @throws IllegalArgumentException if eventType is null
     */
    public Event dequeue(EventType eventType) throws InterruptedException {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        BlockingQueue<Event> queue = getQueue(eventType);
        Event event = queue.take(); // blocks until available

        log.debug("Event {} dequeued from {} queue", event.getEventId(), eventType);
        return event;
    }

    /**
     * Get the queue for a specific event type
     *
     * @param eventType the event type
     * @return the blocking queue for the event type
     * @throws IllegalArgumentException if eventType is null or unsupported
     */
    public BlockingQueue<Event> getQueue(EventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        BlockingQueue<Event> queue = queues.get(eventType);
        if (queue == null) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }

        return queue;
    }

    /**
     * Get the current size of a specific queue
     *
     * @param eventType the event type
     * @return the current number of events in the queue
     */
    public int getQueueSize(EventType eventType) {
        return getQueue(eventType).size();
    }

    /**
     * Get the remaining capacity of a specific queue
     *
     * @param eventType the event type
     * @return the number of additional events the queue can accept
     */
    public int getRemainingCapacity(EventType eventType) {
        return getQueue(eventType).remainingCapacity();
    }

    /**
     * Check if a specific queue is empty
     *
     * @param eventType the event type
     * @return true if the queue is empty
     */
    public boolean isEmpty(EventType eventType) {
        return getQueue(eventType).isEmpty();
    }

    /**
     * Check if a specific queue is full
     *
     * @param eventType the event type
     * @return true if the queue is full
     */
    public boolean isFull(EventType eventType) {
        return getRemainingCapacity(eventType) == 0;
    }

    // JMX Managed Attributes for monitoring

    @ManagedAttribute(description = "Current size of EMAIL queue")
    public int getEmailQueueSize() {
        return getQueueSize(EventType.EMAIL);
    }

    @ManagedAttribute(description = "Current size of SMS queue")
    public int getSmsQueueSize() {
        return getQueueSize(EventType.SMS);
    }

    @ManagedAttribute(description = "Current size of PUSH queue")
    public int getPushQueueSize() {
        return getQueueSize(EventType.PUSH);
    }

    @ManagedAttribute(description = "Remaining capacity of EMAIL queue")
    public int getEmailQueueRemainingCapacity() {
        return getRemainingCapacity(EventType.EMAIL);
    }

    @ManagedAttribute(description = "Remaining capacity of SMS queue")
    public int getSmsQueueRemainingCapacity() {
        return getRemainingCapacity(EventType.SMS);
    }

    @ManagedAttribute(description = "Remaining capacity of PUSH queue")
    public int getPushQueueRemainingCapacity() {
        return getRemainingCapacity(EventType.PUSH);
    }

    @ManagedAttribute(description = "Total number of events across all queues")
    public int getTotalQueueSize() {
        return getEmailQueueSize() + getSmsQueueSize() + getPushQueueSize();
    }
}
