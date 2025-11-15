package com.chatwolf.consumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "messages",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_event_id",
                        columnNames = {"event_id"}),
        indexes = {
            @Index(name = "idx_conversation_seq_desc", columnList = "conversation_id, seq_no DESC"),
            @Index(name = "idx_sender_conversation", columnList = "sender_id, conversation_id, created_at DESC"),
            @Index(name = "idx_recipient_conversation", columnList = "recipient_id, conversation_id, created_at DESC"),
            @Index(name = "idx_client_msg_id", columnList = "client_msg_id"),
            @Index(name = "idx_event_id", columnList = "event_id"),
            @Index(name = "idx_recipient_unread", columnList = "recipient_id, conversation_id, read_at")
        })
@Getter
@Setter
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "client_msg_id")
    private String clientMsgId;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "recipient_id")
    private String recipientId;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "seq_no")
    private Long seqNo;

    @Transient
    @Builder.Default
    private boolean duplicate = false;

    public boolean isDuplicate() {
        return duplicate;
    }

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
