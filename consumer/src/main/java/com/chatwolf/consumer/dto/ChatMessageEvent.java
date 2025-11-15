package com.chatwolf.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.*;

@Getter
@Setter
public class ChatMessageEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("clientMsgId")
    private String clientMsgId;

    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("conversationId")
    private String conversationId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("attachmentUrl")
    private String attachmentUrl;

    @JsonProperty("sentAt")
    private Instant sentAt;
}
