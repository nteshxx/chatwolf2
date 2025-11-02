package com.chatwolf.api.dto;

import java.time.Instant;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private Long id;
    private String chatName;
    private Boolean isGroup;
    private List<UserDTO> participants;
    private Instant createdAt;
}
