package com.chatwolf.consumer.repository;

import com.chatwolf.consumer.entity.Message;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find message by event ID for idempotency check
     * Uses: uk_event_id
     */
    Optional<Message> findByEventId(String eventId);

    /**
     * Check if message already exists by client message ID
     * Uses: idx_client_msg_id
     */
    Optional<Message> findByClientMsgId(String clientMsgId);

    /**
     * Get next sequence number using PostgreSQL function
     * This is atomic and handles concurrency automatically
     */
    @Query("SELECT chatwolf.get_next_conversation_seq(:conversationId)")
    Long getNextSeqNo(@Param("conversationId") String conversationId);
}
