package com.chatwolf.presence.service;

import com.chatwolf.presence.constant.PresenceStatus;
import com.chatwolf.presence.dto.PresenceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final ReactiveStringRedisTemplate reactiveRedis;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisContainer;
    private final MeterRegistry meterRegistry;

    @Value("${presence.redis.key-prefix}")
    private String keyPrefix;

    @Value("${presence.redis.ttl-seconds}")
    private long ttlSeconds;

    @Value("${presence.redis.channel}")
    private String redisChannel;

    @Value("${presence.redis.online-users-key}")
    private String onlineUsersKey;

    @Value("${presence.idempotency.ttl-minutes}")
    private long idempotencyTtlMinutes;

    // Reactive sink for SSE streaming
    private final Sinks.Many<PresenceEvent> sink = Sinks.many().multicast().onBackpressureBuffer(1000, false);

    // Metrics
    private final Counter eventsProcessed;
    private final Counter eventsIgnoredDuplicate;
    private final Counter eventsIgnoredStale;
    private final Counter redisErrors;
    private final AtomicLong onlineUsersCount = new AtomicLong(0);

    public PresenceService(
            StringRedisTemplate redisTemplate,
            ReactiveStringRedisTemplate reactiveRedis,
            ObjectMapper objectMapper,
            RedisMessageListenerContainer redisContainer,
            MeterRegistry meterRegistry) {

        this.redisTemplate = redisTemplate;
        this.reactiveRedis = reactiveRedis;
        this.objectMapper = objectMapper;
        this.redisContainer = redisContainer;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.eventsProcessed = Counter.builder("presence.events.processed")
                .description("Total number of presence events processed successfully")
                .register(meterRegistry);

        this.eventsIgnoredDuplicate = Counter.builder("presence.events.ignored.duplicate")
                .description("Number of duplicate events ignored")
                .register(meterRegistry);

        this.eventsIgnoredStale = Counter.builder("presence.events.ignored.stale")
                .description("Number of stale events ignored")
                .register(meterRegistry);

        this.redisErrors = Counter.builder("presence.redis.errors")
                .description("Number of Redis operation errors")
                .register(meterRegistry);

        // Register gauge for online users count
        Gauge.builder("presence.users.online", onlineUsersCount, AtomicLong::get)
                .description("Current number of online users")
                .register(meterRegistry);

        log.info("PresenceService initialized with metrics");
    }

    @PostConstruct
    public void initRedisSubscription() {
        MessageListener listener = (message, pattern) -> {
            try {
                String json = new String(message.getBody());
                PresenceEvent event = objectMapper.readValue(json, PresenceEvent.class);
                log.debug(
                        "Received presence event from Redis pub/sub: userId={}, status={}",
                        event.getUserId(),
                        event.getStatus());

                processPresenceEvent(event);

            } catch (Exception ex) {
                log.error("Failed to process Redis pub/sub message", ex.getMessage());
                redisErrors.increment();
            }
        };

        ChannelTopic topic = new ChannelTopic(redisChannel);
        redisContainer.addMessageListener(listener, topic);
        log.info("Subscribed to Redis channel: {}", redisChannel);
    }

    /**
     * Process presence event with idempotency and timestamp checks
     */
    private void processPresenceEvent(PresenceEvent event) {
        // Idempotency check
        if (!isEventNew(event.getEventId())) {
            log.debug("Duplicate event ignored: eventId={}", event.getEventId());
            eventsIgnoredDuplicate.increment();
            return;
        }

        // Timestamp-based stale event check
        if (isEventStale(event)) {
            log.debug("Stale event ignored: userId={}, eventTimestamp={}", event.getUserId(), event.getTimestamp());
            eventsIgnoredStale.increment();
            return;
        }

        // Update presence state in Redis
        updatePresenceState(event);

        // 4. Emit to SSE subscribers
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.warn("Failed to emit event to SSE subscribers: {}", result);
        }

        // 5. Update metrics
        eventsProcessed.increment();
        meterRegistry
                .counter(
                        "presence.events.by_status",
                        "status",
                        event.getStatus().name().toUpperCase())
                .increment();
    }

    /**
     * Check if event is new (idempotency)
     */
    private boolean isEventNew(String eventId) {
        String idempotencyKey = "presence:processed:" + eventId;

        try {
            Boolean isNew = redisTemplate
                    .opsForValue()
                    .setIfAbsent(idempotencyKey, "1", Duration.ofMinutes(idempotencyTtlMinutes));
            return Boolean.TRUE.equals(isNew);
        } catch (Exception ex) {
            log.error("Failed to check idempotency for eventId={}", eventId, ex);
            redisErrors.increment();
            return true;
        }
    }

    /**
     * Check if event is stale based on timestamp
     */
    private boolean isEventStale(PresenceEvent event) {
        String timestampKey = keyPrefix + event.getUserId() + ":ts";

        try {
            String currentTsStr = redisTemplate.opsForValue().get(timestampKey);
            if (currentTsStr != null) {
                Instant currentTs = Instant.parse(currentTsStr);
                return event.getTimestamp().isBefore(currentTs);
            }
            return false;
        } catch (Exception ex) {
            log.error("Failed to check timestamp for userId={}", event.getUserId(), ex);
            redisErrors.increment();
            return false;
        }
    }

    /**
     * Update presence state in Redis
     */
    private void updatePresenceState(PresenceEvent event) {
        String userKey = keyPrefix + event.getUserId();
        String timestampKey = userKey + ":ts";

        try {
            if (event.getStatus() == PresenceStatus.OFFLINE) {
                // Remove from online users
                redisTemplate.delete(Arrays.asList(userKey, timestampKey));
                redisTemplate.opsForSet().remove(onlineUsersKey, event.getUserId());

                log.debug("User went offline: userId={}", event.getUserId());
            } else {
                // Update status and timestamp
                redisTemplate
                        .opsForValue()
                        .set(userKey, event.getStatus().name().toUpperCase());
                redisTemplate
                        .opsForValue()
                        .set(timestampKey, event.getTimestamp().toString());

                // Set TTL
                redisTemplate.expire(userKey, Duration.ofSeconds(ttlSeconds));
                redisTemplate.expire(timestampKey, Duration.ofSeconds(ttlSeconds));

                // Add to online users set
                redisTemplate.opsForSet().add(onlineUsersKey, event.getUserId());
                redisTemplate.expire(onlineUsersKey, Duration.ofSeconds(ttlSeconds + 60));

                log.debug("User status updated: userId={}, status={}", event.getUserId(), event.getStatus());
            }
        } catch (Exception ex) {
            log.error("Failed to update presence state for userId={}", event.getUserId(), ex);
            redisErrors.increment();
        }
    }

    /**
     * Get presence status for a user (reactive)
     */
    public Mono<String> getPresence(String userId) {
        String key = keyPrefix + userId;

        return reactiveRedis
                .opsForValue()
                .get(key)
                .defaultIfEmpty(PresenceStatus.OFFLINE.name().toUpperCase())
                .doOnError(ex -> {
                    log.error("Failed to get presence for userId={}", userId, ex);
                    redisErrors.increment();
                })
                .onErrorReturn(PresenceStatus.OFFLINE.name().toUpperCase());
    }

    /**
     * Get online users count
     */
    public Mono<Long> getOnlineUsersCount() {
        return reactiveRedis
                .opsForSet()
                .size(onlineUsersKey)
                .defaultIfEmpty(0L)
                .doOnError(ex -> {
                    log.error("Failed to get online users count", ex);
                    redisErrors.increment();
                })
                .onErrorReturn(0L);
    }

    /**
     * Subscribe to presence events (for SSE)
     */
    public Flux<PresenceEvent> subscribeToPresence() {
        return sink.asFlux()
                .doOnSubscribe(sub -> log.debug("New SSE subscriber connected"))
                .doOnCancel(() -> log.debug("SSE subscriber disconnected"));
    }

    /**
     * Scheduled task to update online users count metric
     */
    @Scheduled(fixedDelayString = "${presence.metrics.update-interval-seconds:30}000")
    public void updateOnlineUsersMetric() {
        getOnlineUsersCount()
                .subscribe(
                        count -> {
                            onlineUsersCount.set(count);
                            log.debug("Updated online users count: {}", count);
                        },
                        ex -> log.error("Failed to update online users metric", ex));
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up PresenceService resources");
        sink.tryEmitComplete();
    }
}
