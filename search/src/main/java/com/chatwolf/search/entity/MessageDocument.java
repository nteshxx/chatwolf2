package com.chatwolf.search.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDocument {
    @Id
    private String id; // eventId

    private String conversationId;
    private String senderId;
    private String content;
    private Long seqNo;
    private Instant sentAt;
}
