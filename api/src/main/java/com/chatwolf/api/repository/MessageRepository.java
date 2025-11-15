package com.chatwolf.api.repository;

import com.chatwolf.api.entity.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Fetch first page of messages for a conversation
     * Uses: idx_conversation_seq_desc
     */
    @Query(
            """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
        ORDER BY m.seqNo DESC
        """)
    List<Message> findFirstPageByConversation(@Param("conversationId") String conversationId, Pageable pageable);

    /**
     * Fetch next page using cursor (sequence number)
     * Uses: idx_conversation_seq_desc
     */
    @Query(
            """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.seqNo < :cursor
        ORDER BY m.seqNo DESC
        """)
    List<Message> findNextPageByConversation(
            @Param("conversationId") String conversationId, @Param("cursor") Long cursor, Pageable pageable);

    // ========== User Conversation Tracking ==========

    /**
     * Get all conversation IDs where user is sender or recipient
     * Uses: idx_sender_conversation, idx_recipient_conversation
     */
    @Query(
            """
        SELECT DISTINCT m.conversationId
        FROM Message m
        WHERE m.senderId = :userId OR m.recipientId = :userId
        ORDER BY m.createdAt DESC
        """)
    List<String> findConversationsByUser(@Param("userId") String userId);

    /**
     * Get conversation list with last message details
     * Uses: idx_sender_conversation, idx_recipient_conversation
     */
    @Query(
            """
        SELECT m.conversationId,
               MAX(m.createdAt) as lastMessageTime,
               (SELECT m2.content FROM Message m2
                WHERE m2.conversationId = m.conversationId
                ORDER BY m2.seqNo DESC LIMIT 1) as lastMessage
        FROM Message m
        WHERE m.senderId = :userId OR m.recipientId = :userId
        GROUP BY m.conversationId
        ORDER BY lastMessageTime DESC
        """)
    List<Object[]> findConversationListByUser(@Param("userId") String userId);

    /**
     * Get last message in a conversation
     * Uses: idx_conversation_seq_desc
     */
    @Query(
            """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
        ORDER BY m.seqNo DESC
        LIMIT 1
        """)
    Optional<Message> findLastMessageInConversation(@Param("conversationId") String conversationId);

    /**
     * Search messages by content (for future full-text search)
     * Uses: idx_conversation_seq_desc + content scan
     */
    @Query(
            """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY m.seqNo DESC
        """)
    List<Message> searchMessagesInConversation(
            @Param("conversationId") String conversationId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count total messages in conversation
     * Uses: idx_conversation_seq_desc
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversationId = :conversationId
        """)
    long countMessagesByConversation(@Param("conversationId") String conversationId);

    /**
     * Get messages between date range
     * Uses: idx_conversation_created_desc
     */
    @Query(
            """
        SELECT m FROM Message m
        WHERE m.conversationId = :conversationId
          AND m.createdAt BETWEEN :startDate AND :endDate
        ORDER BY m.seqNo ASC
        """)
    List<Message> findMessagesByDateRange(
            @Param("conversationId") String conversationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
