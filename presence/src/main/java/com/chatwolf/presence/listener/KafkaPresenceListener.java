package com.chatwolf.presence.listener;

import com.chatwolf.presence.dto.PresenceEvent;
import com.chatwolf.presence.service.PresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaPresenceListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaPresenceListener.class);

    private final PresenceService presenceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${presence.kafka.topic:presence-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, String> rec) {
        try {
            String json = rec.value();
            PresenceEvent ev = objectMapper.readValue(json, PresenceEvent.class);
            log.debug("Received presence event for user {}: {}", ev.getUserId(), ev.getStatus());
            presenceService.handleEventFromKafka(ev);
        } catch (Exception ex) {
            log.error("Failed to process presence event", ex);
            throw new RuntimeException(ex);
        }
    }
}
