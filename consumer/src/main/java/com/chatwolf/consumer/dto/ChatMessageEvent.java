package com.chatwolf.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {
    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("clientMsgId")
    private String clientMsgId;

    @JsonProperty("from")
    private String senderId;

    @JsonProperty("to")
    private String receiverId;

    @JsonProperty("conversationId")
    private String conversationId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("attachmentUrl")
    private String attachmentUrl;

    @JsonProperty("sentAt")
    private Instant sentAt;
}
