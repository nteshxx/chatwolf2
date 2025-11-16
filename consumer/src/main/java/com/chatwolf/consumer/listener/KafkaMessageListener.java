package com.chatwolf.consumer.listener;

import com.chatwolf.consumer.dto.ChatMessageEvent;
import com.chatwolf.consumer.entity.Message;
import com.chatwolf.consumer.exception.NonRecoverableException;
import com.chatwolf.consumer.exception.RecoverableException;
import com.chatwolf.consumer.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class KafkaMessageListener {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter messagesProcessed;
    private Counter messagesFailed;
    private Counter duplicateMessages;
    private Timer messageProcessingTimer;

    @PostConstruct
    public void initMetrics() {
        messagesProcessed = Counter.builder("kafka.messages.processed")
                .description("Total messages successfully processed")
                .tag("topic", "chat-messages")
                .register(meterRegistry);

        messagesFailed = Counter.builder("kafka.messages.failed")
                .description("Total messages failed to process")
                .tag("topic", "chat-messages")
                .register(meterRegistry);

        duplicateMessages = Counter.builder("kafka.messages.duplicate")
                .description("Total duplicate messages detected")
                .tag("topic", "chat-messages")
                .register(meterRegistry);

        messageProcessingTimer = Timer.builder("kafka.message.processing.time")
                .description("Message processing time")
                .tag("topic", "chat-messages")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "${kafka.consumer.concurrency:3}")
    public void listen(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            Acknowledgment acknowledgment) {

        Instant startTime = Instant.now();

        try {
            log.info("Processing message - key={}, partition={}, offset={}", key, partition, offset);

            ChatMessageEvent event = deserializeMessage(payload);

            validateEvent(event);

            Message message = processMessage(event);

            if (message.isDuplicate()) {
                duplicateMessages.increment();
                log.warn("Duplicate message detected - eventId={}", event.getEventId());
            }

            // Manual acknowledgment after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            messagesProcessed.increment();
            recordMessageProcessingTime(startTime);

            log.info(
                    "Successfully processed message - eventId={}, duration={}ms",
                    event.getEventId(),
                    Duration.between(startTime, Instant.now()).toMillis());

        } catch (NonRecoverableException e) {
            // Don't retry - send to DLQ
            messagesFailed.increment();
            log.error("Non-recoverable error processing message - will send to DLQ", e);

            if (acknowledgment != null) {
                acknowledgment.acknowledge(); // Acknowledge to prevent retry
            }
            // Exception will be handled by error handler and sent to DLQ
            throw e;

        } catch (RecoverableException e) {
            // Retry - don't acknowledge
            messagesFailed.increment();
            log.warn("Recoverable error processing message - will retry", e);
            throw e; // Will trigger retry based on configuration

        } catch (Exception e) {
            // Unknown error - treat as recoverable with limited retries
            messagesFailed.increment();
            log.error("Unexpected error processing message", e);
            throw new RecoverableException("Unexpected error", e);
        }
    }

    private ChatMessageEvent deserializeMessage(String payload) {
        try {
            if (payload == null || payload.trim().isEmpty()) {
                throw new NonRecoverableException("Empty or null message payload");
            }

            ChatMessageEvent event = objectMapper.readValue(payload, ChatMessageEvent.class);

            if (event == null) {
                throw new NonRecoverableException("Deserialized event is null");
            }

            return event;

        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("Failed to deserialize message: " + e.getMessage(), e);
        }
    }

    private void validateEvent(ChatMessageEvent event) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new NonRecoverableException("Event ID is required");
        }

        if (event.getContent() == null) {
            throw new NonRecoverableException("Message content is required");
        }

        // Add more validation as needed
        if (event.getTo() == null || event.getTo() == null) {
            throw new NonRecoverableException("Sender and receiver IDs are required");
        }
    }

    private Message processMessage(ChatMessageEvent event) {
        try {
            Message saved = messageService.saveMessage(event);

            log.debug(
                    "Persisted message - id={}, seqNo={}, isDuplicate={}",
                    saved.getId(),
                    saved.getSeqNo(),
                    saved.isDuplicate());

            return saved;

        } catch (DataAccessException e) {
            // Database errors are usually recoverable
            throw new RecoverableException("Database error while persisting message", e);
        } catch (Exception e) {
            // Other persistence errors
            log.error("Error persisting message", e);
            throw new RecoverableException("Failed to persist message", e);
        }
    }

    private void recordMessageProcessingTime(Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        messageProcessingTimer.record(duration);
    }
}
