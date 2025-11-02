package com.chatwolf.presence.service;

import com.chatwolf.presence.dto.PresenceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.listener.RedisMessageListenerContainer redisContainer;

    @Value("${presence.redis.key-prefix:presence:user:}")
    private String keyPrefix;

    @Value("${presence.redis.ttl-seconds:120}")
    private long ttlSeconds;

    @Value("${presence.kafka.topic:presence-events}")
    private String kafkaTopic;

    private final Sinks.Many<PresenceEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void handleEventFromKafka(PresenceEvent ev) {
        // write state into redis with TTL
        String key = keyPrefix + ev.getUserId();
        if ("offline".equalsIgnoreCase(ev.getStatus())) {
            // remove key
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, ev.getStatus());
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }

        // publish to redis pubsub channel so other instances / socket nodes can subscribe
        try {
            String payload = objectMapper.writeValueAsString(ev);
            redisTemplate.convertAndSend(kafkaTopic, payload);
        } catch (Exception ex) {
            // log, but continue
            ex.printStackTrace();
        }

        // emit to local SSE subscribers
        sink.tryEmitNext(ev);
    }

    public Optional<String> getPresence(String userId) {
        String key = keyPrefix + userId;
        String val = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(val);
    }

    // SSE subscription flux
    public reactor.core.publisher.Flux<PresenceEvent> subscribeAll() {
        return sink.asFlux();
    }

    @PostConstruct
    public void initRedisSubscription() {
        // subscribe Redis channel to receive presence events from other instances
        MessageListener listener = (message, pattern) -> {
            try {
                String json = new String(message.getBody());
                PresenceEvent ev = objectMapper.readValue(json, PresenceEvent.class);
                // push to local SSE sink
                sink.tryEmitNext(ev);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
        ChannelTopic topic = new ChannelTopic(kafkaTopic);
        redisContainer.addMessageListener(listener, topic);
    }
}
