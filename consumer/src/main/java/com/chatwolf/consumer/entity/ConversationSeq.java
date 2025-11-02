package com.chatwolf.consumer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversation_seq")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSeq {
    @Id
    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "last_seq")
    private Long lastSeq;
}
