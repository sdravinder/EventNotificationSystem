package com.ravinder.eventnotificationsystem.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SMS event implementation for sending text message notifications.
 * Processing time: 3 seconds per event.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("SMS")
public class SmsEvent extends Event {

    /**
     * SMS payload containing phone number and message information
     */
    private SmsPayload payload;

    /**
     * Constructor for creating a new SMS event
     */
    public SmsEvent(String callbackUrl, SmsPayload payload) {
        super(EventType.SMS, callbackUrl);
        this.payload = payload;
    }

    @Override
    public SmsPayload getPayload() {
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
