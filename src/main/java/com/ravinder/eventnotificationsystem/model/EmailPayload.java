package com.ravinder.eventnotificationsystem.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailPayload {
    
    @NotBlank
    @Email(message = "Invalid email format")
    private String recipient;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    /**
     * Validate the email payload
     */
    public boolean isValid() {
        return recipient != null && !recipient.trim().isEmpty() &&
                recipient.contains("@") &&
                message != null && !message.trim().isEmpty() &&
                message.length() <= 1000;
    }
}
