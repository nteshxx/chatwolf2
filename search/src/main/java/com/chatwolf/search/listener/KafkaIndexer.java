package com.chatwolf.search.listener;

import com.chatwolf.search.dto.ChatMessageEvent;
import com.chatwolf.search.entity.MessageDocument;
import com.chatwolf.search.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaIndexer {

    private final MessageRepository repo;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    private static final String METRIC_PREFIX = "chatwolf.search.kafka";
    private static final String CIRCUIT_BREAKER_NAME = "messageIndexing";

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class})
    @KafkaListener(topics = "${KAFKA_TOPIC:chat-messages}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Processing message from topic: {}, partition: {}, offset: {}", topic, partition, offset);

            ChatMessageEvent event = parseEvent(payload);

            if (!isValidEvent(event)) {
                incrementCounter("validation.failure");
                log.warn("Received invalid ChatMessageEvent: {}", payload);
                return;
            }

            indexMessage(event);
            incrementCounter("processing.success");
            log.info(
                    "Successfully indexed message: eventId={}, conversationId={}",
                    event.getEventId(),
                    event.getConversationId());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            incrementCounter("deserialization.failure");
            log.error("Failed to deserialize message payload", e);
            throw new IllegalArgumentException("Invalid JSON payload", e);
        } catch (Exception e) {
            incrementCounter("processing.failure");
            log.error(
                    "Error processing Kafka message from topic: {}, partition: {}, offset: {}",
                    topic,
                    partition,
                    offset,
                    e);
            throw new RuntimeException("Failed to process message", e);
        } finally {
            sample.stop(Timer.builder(METRIC_PREFIX + ".processing.duration")
                    .description("Message processing time")
                    .register(meterRegistry));
        }
    }

    @KafkaListener(topics = "${KAFKA_TOPIC:chat-messages}.DLT")
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        incrementCounter("dlt.received");
        log.error(
                "Message sent to Dead Letter Topic. Topic: {}, Exception: {}, Payload: {}",
                topic,
                exceptionMessage,
                payload);
        // TODO: Implement alerting mechanism (e.g., send to monitoring system, email
        // alerts)
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "indexMessageFallback")
    private void indexMessage(ChatMessageEvent event) {
        MessageDocument doc = buildMessageDocument(event);
        repo.save(doc);
    }

    public void indexMessageFallback(ChatMessageEvent event, Exception ex) {
        incrementCounter("circuit.breaker.fallback");
        log.error("Circuit breaker opened for event: {}, using fallback mechanism", event.getEventId(), ex);
        // TODO: Implement fallback strategy (e.g., queue to cache, write to backup
        // store)
        throw new RuntimeException("Service unavailable - circuit breaker open", ex);
    }

    private ChatMessageEvent parseEvent(String payload) throws com.fasterxml.jackson.core.JsonProcessingException {
        return mapper.readValue(payload, ChatMessageEvent.class);
    }

    private boolean isValidEvent(ChatMessageEvent event) {
        return event != null
                && event.getEventId() != null
                && !event.getEventId().isBlank()
                && event.getConversationId() != null
                && !event.getConversationId().isBlank()
                && event.getFrom() != null
                && !event.getFrom().isBlank()
                && event.getContent() != null
                && !event.getContent().isBlank()
                && event.getSentAt() != null;
    }

    private MessageDocument buildMessageDocument(ChatMessageEvent event) {
        return MessageDocument.builder()
                .id(event.getEventId())
                .conversationId(event.getConversationId())
                .senderId(event.getFrom())
                .content(event.getContent())
                .seqNo(System.currentTimeMillis())
                .sentAt(event.getSentAt())
                .build();
    }

    private void incrementCounter(String name) {
        Counter.builder(METRIC_PREFIX + "." + name)
                .description("Count of " + name)
                .register(meterRegistry)
                .increment();
    }
}
