package com.chatwolf.notification.listener;

import com.chatwolf.notification.dto.NotificationEvent;
import com.chatwolf.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "${NOTIFICATION_TOPIC:notification-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String payload) {
        try {
            NotificationEvent ev = mapper.readValue(payload, NotificationEvent.class);
            notificationService.process(ev);
        } catch (Exception ex) {
            log.error("failed processing notification", ex);
            throw new RuntimeException(ex); // let kafka retry / DLQ
        }
    }
}
