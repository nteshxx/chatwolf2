package com.chatwolf.notification.dto;

import com.chatwolf.notification.constant.NotificationType;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private NotificationType type;
    private String recipient;
    private String subject;
    private String body;
    private Map<String, Object> meta;
}
