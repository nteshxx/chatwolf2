package com.chatwolf.notification.listener;

import com.chatwolf.notification.constant.Constants;
import com.chatwolf.notification.dto.NotificationEvent;
import com.chatwolf.notification.service.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class KafkaNotificationListener {

    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter notificationsProcessed;
    private Counter notificationsFailed;
    private Timer notificationProcessingTimer;

    @PostConstruct
    public void initMetrics() {
        notificationsProcessed = Counter.builder("kafka.notifications.processed")
                .description("Total notifications successfully processed")
                .tag("topic", Constants.KAFKA_NOTIFICATION_EVENTS_TOPIC)
                .register(meterRegistry);

        notificationsFailed = Counter.builder("kafka.notifications.failed")
                .description("Total notifications failed to process")
                .tag("topic", Constants.KAFKA_NOTIFICATION_EVENTS_TOPIC)
                .register(meterRegistry);

        notificationProcessingTimer = Timer.builder("kafka.notifications.processing.time")
                .description("Notification processing time")
                .tag("topic", Constants.KAFKA_NOTIFICATION_EVENTS_TOPIC)
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = Constants.KAFKA_NOTIFICATION_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "${kafka.consumer.concurrency:3}")
    public void listen(
            @Valid NotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            Acknowledgment acknowledgment) {

        Instant startTime = Instant.now();

        try {
            log.info("Processing message - key={}, partition={}, offset={}", key, partition, offset);

            notificationService.process(event);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            notificationsProcessed.increment();

            Duration duration = Duration.between(startTime, Instant.now());
            notificationProcessingTimer.record(duration);

            log.info(
                    "Successfully processed notification - duration={}ms",
                    Duration.between(startTime, Instant.now()).toMillis());

        } catch (Exception ex) {
            notificationsFailed.increment();
            log.error("failed processing notification: " + ex.getMessage(), ex);

            // No retry for failed notifications
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
}
