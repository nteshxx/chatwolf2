package com.chatwolf.auth.service;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send message asynchronously with callback
     */
    public void sendMessage(String topic, String key, Object message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(
                        "Notification sent successfully to topic: {}, partition: {}, offset: {}, key: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            } else {
                log.error("Failed to send message to topic: {}, key: {}", topic, key, ex);
            }
        });
    }

    /**
     * Send message without key
     */
    public void sendMessage(String topic, Object message) {
        sendMessage(topic, null, message);
    }

    /**
     * Send message synchronously (blocks until acknowledged)
     */
    public SendResult<String, Object> sendMessageSync(String topic, String key, Object message) {
        try {
            SendResult<String, Object> result =
                    kafkaTemplate.send(topic, key, message).get();
            log.info(
                    "Message sent synchronously to topic: {}, partition: {}, offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return result;
        } catch (Exception e) {
            log.error("Error sending message synchronously to topic: {}", topic, e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Send message to specific partition
     */
    public void sendMessageToPartition(String topic, int partition, String key, Object message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, partition, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(
                        "Message sent to partition: {}, offset: {}",
                        partition,
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to partition: {}", partition, ex);
            }
        });
    }
}
