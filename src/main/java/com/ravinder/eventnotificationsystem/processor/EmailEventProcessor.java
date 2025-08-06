package com.ravinder.eventnotificationsystem.processor;


import com.ravinder.eventnotificationsystem.config.EventNotificationProperties;
import com.ravinder.eventnotificationsystem.model.EmailEvent;
import com.ravinder.eventnotificationsystem.model.EmailPayload;
import com.ravinder.eventnotificationsystem.model.EventType;
import com.ravinder.eventnotificationsystem.queue.EventQueueManager;
import com.ravinder.eventnotificationsystem.service.CallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Processor for EMAIL events.
 * Processes email notifications with a 5-second delay per event.
 * <p>
 * This class extends EventProcessor and implements the EventProcessor
 */
@Component
@Slf4j
@ManagedResource(objectName = "com.ravinder.eventnotificationsystem:type=EmailEventProcessor")
public class EmailEventProcessor extends EventProcessor<EmailEvent> {

    /**
     * Constructor for EmailEventProcessor
     *
     * @param queueManager the queue manager for dequeuing events
     * @param callbackService the callback service for notifications
     * @param properties   configuration properties for processing delays and failure rates
     */
    @Autowired
    public EmailEventProcessor(EventQueueManager queueManager,
                               CallbackService callbackService,
                               EventNotificationProperties properties) {
        super(queueManager,
                callbackService,
                EventType.EMAIL,
                properties.getProcessing().getEmail().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());

        log.info("Initialized EmailEventProcessor with {}s delay and {}% failure rate",
                properties.getProcessing().getEmail().getDelaySeconds(),
                properties.getProcessing().getFailureRatePercent());
    }

    /**
     * Process email-specific event logic.
     * This method contains the business logic for sending email notifications.
     *
     * @param event the email event to process
     * @throws Exception if email processing fails
     */
    @Override
    protected void processSpecificEvent(EmailEvent event) throws Exception {
        EmailPayload payload = event.getPayload();

        log.info("Processing email event {} - Sending email to: {}",
                event.getEventId(), payload.getRecipient());

        // Simulate email sending logic
        validateEmailPayload(payload);
        sendEmail(payload);

        log.debug("Email sent successfully for event {} to recipient: {}",
                event.getEventId(), payload.getRecipient());
    }

    /**
     * Validate email payload before processing
     *
     * @param payload the email payload to validate
     * @throws IllegalArgumentException if payload is invalid
     */
    private void validateEmailPayload(EmailPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Email payload cannot be null");
        }

        if (payload.getRecipient() == null || payload.getRecipient().trim().isEmpty()) {
            throw new IllegalArgumentException("Email recipient cannot be null or empty");
        }

        if (!payload.getRecipient().contains("@")) {
            throw new IllegalArgumentException("Invalid email format: " + payload.getRecipient());
        }

        if (payload.getMessage() == null || payload.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Email message cannot be null or empty");
        }

        if (payload.getMessage().length() > 1000) {
            throw new IllegalArgumentException("Email message too long: " + payload.getMessage().length() + " characters");
        }
    }

    /**
     * Simulate sending an email
     * In a real implementation, this would integrate with email service providers
     * like SendGrid, AWS SES, or SMTP servers.
     *
     * @param payload the email payload containing recipient and message
     * @throws Exception if email sending fails
     */
    private void sendEmail(EmailPayload payload) throws Exception {
        // Simulate email service integration
        log.debug("Connecting to email service...");

        // Simulate potential service failures
        if (payload.getRecipient().contains("invalid")) {
            throw new Exception("Invalid email address rejected by email service");
        }

        log.debug("Sending email:");
        log.debug("  To: {}", payload.getRecipient());
        log.debug("  Message: {}", payload.getMessage().substring(0, Math.min(50, payload.getMessage().length())) + "...");

        // Simulate email delivery
        log.debug("Email delivered successfully");
    }

    @Override
    protected String getThreadName() {
        return "email-processor-thread";
    }
}
