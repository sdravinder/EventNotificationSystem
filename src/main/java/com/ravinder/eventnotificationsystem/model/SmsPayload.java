package com.ravinder.eventnotificationsystem.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SMS payload data structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsPayload {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format. Use international format (e.g., +1234567890)")
    private String phoneNumber;

    @NotBlank(message = "Message is required")
    @Size(max = 160, message = "SMS message cannot exceed 160 characters")
    private String message;

    /**
     * Validate the SMS payload
     */
    public boolean isValid() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty() &&
                phoneNumber.matches("^\\+[1-9]\\d{1,14}$") &&
                message != null && !message.trim().isEmpty() &&
                message.length() <= 160;
    }
}
