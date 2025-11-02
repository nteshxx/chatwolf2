package com.chatwolf.consumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId; // id from socket layer

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
    private Long seqNo; // per-conversation sequence

    @Transient
    @Builder.Default
    private boolean duplicate = false;

    public boolean isDuplicate() {
        return duplicate;
    }

    @Column(name = "created_at")
    private Instant createdAt;
}
