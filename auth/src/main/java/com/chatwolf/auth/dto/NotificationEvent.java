package com.chatwolf.auth.dto;

import com.chatwolf.auth.constant.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    @NotNull
    private NotificationType type;

    @Email(message = "Invalid email format")
    private String recipient;

    private String username;

    private String otp;

    private Map<String, Object> meta;
}
