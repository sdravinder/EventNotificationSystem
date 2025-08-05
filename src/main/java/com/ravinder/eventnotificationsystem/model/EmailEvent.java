package com.ravinder.eventnotificationsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * Email event implementation for sending email notifications.
 * Processing time: 5 seconds per event.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent extends Event {

    /**
     * Email payload containing recipient and message information
     */
    private EmailPayload payload;

    /**
     * Constructor for creating a new email event
     */
    public EmailEvent(String callbackUrl, EmailPayload payload) {
        super(EventType.EMAIL, callbackUrl);
        this.payload = payload;
    }

    @Override
    public EmailPayload getPayload() {
        return this.payload;
    }

    @Override
    public boolean isValidPayload() {
        if (payload == null) {
            return false;
        }
        return payload.isValid();
    }
}
