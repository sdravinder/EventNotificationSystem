package com.ravinder.eventnotificationsystem.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushPayload {

    @NotBlank(message = "Device ID is required")
    @Size(min = 3, max = 255, message = "Device ID must be between 3 and 255 characters")
    private String deviceId;

    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Push notification message cannot exceed 500 characters")
    private String message;


    /**
     * Validate the push notification payload
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty() &&
                deviceId.length() >= 3 && deviceId.length() <= 255 &&
                message != null && !message.trim().isEmpty() &&
                message.length() <= 500;
    }
}