package com.chatwolf.api.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationSummary {
    private String conversationId;
    private Instant lastMessageTime;
    private String lastMessagePreview;
    private long unreadCount;
}
