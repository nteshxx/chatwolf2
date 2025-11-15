package com.chatwolf.api.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class MessageResponse {
    private Long id;
    private String conversationId;
    private String senderId;
    private String recipientId;
    private String content;
    private String attachmentUrl;
    private Long seqNo;
    private Instant createdAt;
}
