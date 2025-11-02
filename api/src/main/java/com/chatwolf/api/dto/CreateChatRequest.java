package com.chatwolf.api.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRequest {
    private String chatName;
    private Boolean isGroup;
    private List<String> participantIds;
}
