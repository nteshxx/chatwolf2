package com.chatwolf.search.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChatMessageEvent represents a message event published to Kafka
 * by the Socket Service. It is consumed by both MessageConsumerService
 * (for persistence) and SearchService (for indexing).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    /**
     * Unique event identifier (UUID or Kafka message key)
     */
    private String eventId;

    /**
     * Conversation or chat room ID the message belongs to
     */
    private String conversationId;

    /**
     * Sender’s user ID
     */
    private String from;

    /**
     * Optional receiver’s user ID (for 1:1 chats)
     */
    private String to;

    /**
     * Actual text content of the message
     */
    private String content;

    /**
     * Optional attachment key (stored in Storage Service / MinIO)
     */
    private String attachmentKey;

    /**
     * Timestamp when message was sent (set by Socket Service)
     */
    private Instant sentAt;

    /**
     * Timestamp when event was produced to Kafka
     */
    private Instant producedAt;

    /**
     * Type of message (e.g., TEXT, IMAGE, FILE)
     */
    private String messageType;

    /**
     * Optional metadata (e.g., emojis, replies, reactions)
     */
    private String metadata;
}
