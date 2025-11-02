package com.chatwolf.api.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private String chatId;
    private UserDTO sender;
    private String content;
    private String attachmentUrl;
    private Instant sentAt;
}
