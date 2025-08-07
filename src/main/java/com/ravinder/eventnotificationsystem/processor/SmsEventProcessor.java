package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.model.SmsEvent;
import com.ravinder.eventnotificationsystem.model.SmsPayload;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import com.ravinder.eventnotificationsystem.service.CallbackService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Processor for SMS events.
 * Processes SMS notifications with a 3-second delay per event.
 * <p>
 * This class extends EventProcessor and implements the Strategy pattern
 * for SMS-specific processing logic.
 */
@Component
@Slf4j
@ManagedResource(objectName = "com.ravinder.eventnotificationsystem:type=SmsEventProcessor")
public class SmsEventProcessor extends EventProcessor<SmsEvent> {

    /**
     * Constructor for SmsEventProcessor
     *
     * @param queueManager the queue manager for dequeuing events
     * @param callbackService the callback service for notifications
     * @param properties   configuration properties for processing delays and failure rates
     */
    @Autowired
    public SmsEventProcessor(EventQueueManager queueManager,
                             CallbackService callbackService,
                             EventNotificationProperties properties) {
        super(queueManager,
                callbackService,
                EventType.SMS,
                properties.getProcessing().getSms().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());

        log.info("Initialized SmsEventProcessor with {}s delay and {}% failure rate",
                properties.getProcessing().getSms().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());
    }

    /**
     * Process SMS-specific event logic.
     * This method contains the business logic for sending SMS notifications.
     *
     * @param event the SMS event to process
     * @throws Exception if SMS processing fails
     */
    @Override
    protected void processSpecificEvent(SmsEvent event) throws Exception {
        SmsPayload payload = event.getPayload();

        log.info("Processing SMS event {} - Sending SMS to: {}",
                event.getEventId(), payload.getPhoneNumber());

        // Simulate SMS sending logic
        validateSmsPayload(payload);
        sendSms(payload);

        log.debug("SMS sent successfully for event {} to phone: {}",
                event.getEventId(), payload.getPhoneNumber());
    }

    /**
     * Validate SMS payload before processing
     *
     * @param payload the SMS payload to validate
     * @throws IllegalArgumentException if payload is invalid
     */
    private void validateSmsPayload(SmsPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("SMS payload cannot be null");
        }

        if (payload.getPhoneNumber() == null || payload.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        if (!payload.getPhoneNumber().matches("^\\+[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format: " + payload.getPhoneNumber());
        }

        if (payload.getMessage() == null || payload.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("SMS message cannot be null or empty");
        }

        if (payload.getMessage().length() > 160) {
            throw new IllegalArgumentException("SMS message too long: " + payload.getMessage().length() + " characters (max 160)");
        }
    }

    /**
     * Simulate sending an SMS
     * In a real implementation, this would integrate with SMS service providers
     * like Twilio, AWS SNS, or telecom gateways.
     *
     * @param payload the SMS payload containing phone number and message
     * @throws Exception if SMS sending fails
     */
    private void sendSms(SmsPayload payload) throws Exception {
        // Simulate SMS service integration
        log.debug("Connecting to SMS service...");

        // Simulate potential service failures
        if (payload.getPhoneNumber().contains("invalid") || payload.getPhoneNumber().equals("+0000000000")) {
            throw new Exception("Invalid phone number rejected by SMS service");
        }

        log.debug("Sending SMS:");
        log.debug("  To: {}", payload.getPhoneNumber());
        log.debug("  Message: {}", payload.getMessage().substring(0, Math.min(50, payload.getMessage().length())) +
                (payload.getMessage().length() > 50 ? "..." : ""));

        // Simulate SMS delivery
        log.debug("SMS delivered successfully");
    }

    @Override
    protected String getThreadName() {
        return "sms-processor-thread";
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Graceful shutdown: stopping SmsEventProcessor");
        stop();
    }
}
