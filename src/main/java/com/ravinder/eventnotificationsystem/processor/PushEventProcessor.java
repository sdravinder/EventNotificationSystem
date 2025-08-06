package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.model.PushEvent;
import com.ravinder.eventnotificationsystem.model.PushPayload;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import com.ravinder.eventnotificationsystem.service.CallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Processor for PUSH events.
 * Processes push notifications with a 2-second delay per event.
 * <p>
 * This class extends EventProcessor and implements the Strategy pattern
 * for push notification-specific processing logic.
 */
@Component
@Slf4j
@ManagedResource(objectName = "com.ravinder.eventnotificationsystem:type=PushEventProcessor")
public class PushEventProcessor extends EventProcessor<PushEvent> {

    /**
     * Constructor for PushEventProcessor
     *
     * @param queueManager the queue manager for dequeuing events
     * @param callbackService the callback service for notifications
     * @param properties   configuration properties for processing delays and failure rates
     */
    @Autowired
    public PushEventProcessor(EventQueueManager queueManager,
                              CallbackService callbackService,
                              EventNotificationProperties properties) {
        super(queueManager,
                callbackService,
                EventType.PUSH,
                properties.getProcessing().getPush().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());

        log.info("Initialized PushEventProcessor with {}s delay and {}% failure rate",
                properties.getProcessing().getPush().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());
    }

    /**
     * Process push notification-specific event logic.
     * This method contains the business logic for sending push notifications.
     *
     * @param event the push event to process
     * @throws Exception if push notification processing fails
     */
    @Override
    protected void processSpecificEvent(PushEvent event) throws Exception {
        PushPayload payload = event.getPayload();

        log.info("Processing push event {} - Sending push notification to device: {}",
                event.getEventId(), payload.getDeviceId());

        // Simulate push notification sending logic
        validatePushPayload(payload);
        sendPushNotification(payload);

        log.debug("Push notification sent successfully for event {} to device: {}",
                event.getEventId(), payload.getDeviceId());
    }

    /**
     * Validate push notification payload before processing
     *
     * @param payload the push payload to validate
     * @throws IllegalArgumentException if payload is invalid
     */
    private void validatePushPayload(PushPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Push payload cannot be null");
        }

        if (payload.getDeviceId() == null || payload.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }

        if (payload.getDeviceId().length() < 3 || payload.getDeviceId().length() > 255) {
            throw new IllegalArgumentException("Device ID must be between 3 and 255 characters: " + payload.getDeviceId().length());
        }

        if (payload.getMessage() == null || payload.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Push message cannot be null or empty");
        }

        if (payload.getMessage().length() > 500) {
            throw new IllegalArgumentException("Push message too long: " + payload.getMessage().length() + " characters (max 500)");
        }
    }

    /**
     * Simulate sending a push notification
     * In a real implementation, this would integrate with push notification services
     * like Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNS),
     * or other mobile push notification providers.
     *
     * @param payload the push payload containing device ID and notification details
     * @throws Exception if push notification sending fails
     */
    private void sendPushNotification(PushPayload payload) throws Exception {
        // Simulate push notification service integration
        log.debug("Connecting to push notification service...");

        // Simulate potential service failures
        if (payload.getDeviceId().contains("invalid") || payload.getDeviceId().equals("000")) {
            throw new Exception("Invalid device ID rejected by push notification service");
        }

        log.debug("Sending push notification:");
        log.debug("  Device ID: {}", payload.getDeviceId());
        log.debug("  Message: {}", payload.getMessage().substring(0, Math.min(50, payload.getMessage().length())) +
                (payload.getMessage().length() > 50 ? "..." : ""));


        // Simulate push notification delivery
        log.debug("Push notification delivered successfully");
    }

    @Override
    protected String getThreadName() {
        return "push-processor-thread";
    }
}
