package com.ravinder.eventnotificationsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * Push notification event implementation for sending push notifications to mobile/web apps.
 * Processing time: 2 seconds per event.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PushEvent extends Event {

    /**
     * Push notification payload containing device ID and message information
     */
    private PushPayload payload;

    public PushEvent(String callbackUrl, PushPayload payload) {
        super(EventType.PUSH, callbackUrl);
        this.payload = payload;
    }

    @Override
    public PushPayload getPayload() {
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
