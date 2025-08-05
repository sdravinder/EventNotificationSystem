package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.model.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory class for managing event processors.
 * This class follows the Factory pattern and provides centralized access
 * to event processors for different event types.
 * <p>
 * SOLID Principles:
 * - Single Responsibility: Factory for creating and managing processors
 * - Dependency Inversion: Depends on abstractions (EventProcessor interface)
 */
@Component
public class EventProcessorFactory {

    private final Map<EventType, EventProcessor<?>> processors;

    /**
     * Constructor that initializes all event processors
     *
     * @param emailProcessor the email event processor
     * @param smsProcessor   the SMS event processor
     * @param pushProcessor  the push event processor
     */
    @Autowired
    public EventProcessorFactory(EmailEventProcessor emailProcessor,
                                 SmsEventProcessor smsProcessor,
                                 PushEventProcessor pushProcessor) {
        this.processors = new EnumMap<>(EventType.class);
        this.processors.put(EventType.EMAIL, emailProcessor);
        this.processors.put(EventType.SMS, smsProcessor);
        this.processors.put(EventType.PUSH, pushProcessor);
    }

    /**
     * Get the processor for a specific event type
     *
     * @param eventType the event type
     * @return the processor for the event type
     * @throws IllegalArgumentException if event type is null or unsupported
     */
    public EventProcessor<?> getProcessor(EventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        EventProcessor<?> processor = processors.get(eventType);
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for event type: " + eventType);
        }

        return processor;
    }

    /**
     * Get all registered processors
     *
     * @return map of event types to their processors
     */
    public Map<EventType, EventProcessor<?>> getAllProcessors() {
        return new EnumMap<>(processors);
    }

    /**
     * Start all processors
     */
    public void startAllProcessors() {
        processors.values().forEach(EventProcessor::start);
    }

    /**
     * Stop all processors
     */
    public void stopAllProcessors() {
        processors.values().forEach(EventProcessor::stop);
    }

    /**
     * Check if all processors are running
     *
     * @return true if all processors are running
     */
    public boolean areAllProcessorsRunning() {
        return processors.values().stream().allMatch(EventProcessor::isRunning);
    }

    /**
     * Get the count of running processors
     *
     * @return number of processors currently running
     */
    public long getRunningProcessorCount() {
        return processors.values().stream().mapToLong(p -> p.isRunning() ? 1 : 0).sum();
    }
}
