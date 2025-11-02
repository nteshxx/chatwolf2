package com.chatwolf.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String senderId;
    private String content;
    private String attachmentUrl; // optional
}
