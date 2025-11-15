package com.chatwolf.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessagePageResponse {
    private List<MessageResponse> messages;
    private Long nextCursor; // Sequence number for next page
    private boolean hasMore; // True if more messages available
    private long totalCount; // Total messages in conversation
}
