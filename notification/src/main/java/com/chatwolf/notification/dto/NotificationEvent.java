package com.chatwolf.notification.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type; // EMAIL, SMS
    private String recipient;
    private String subject;
    private String body;
    private Map<String, Object> meta;
}
