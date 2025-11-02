package com.chatwolf.api.service;

import com.chatwolf.api.dto.ChatDTO;
import com.chatwolf.api.dto.CreateChatRequest;
import com.chatwolf.api.dto.UserDTO;
import com.chatwolf.api.entity.Chat;
import com.chatwolf.api.repository.ChatRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final AuthService authService;

    public List<ChatDTO> getUserChats(String userId) {
        return chatRepository.findByParticipantIdsContaining(userId).stream()
                .map(chat -> ChatDTO.builder()
                        .id(chat.getId())
                        .chatName(chat.getChatName())
                        .isGroup(chat.getIsGroup())
                        .participants(chat.getParticipantIds().stream()
                                .map(authService::getUserById)
                                .map(user -> user.getBody())
                                .collect(Collectors.toList()))
                        .createdAt(chat.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public ChatDTO createChat(CreateChatRequest req) {
        Chat chat = Chat.builder()
                .chatName(req.getChatName())
                .isGroup(req.getIsGroup())
                .participantIds(req.getParticipantIds())
                .build();
        chatRepository.save(chat);

        List<UserDTO> users = req.getParticipantIds().stream()
                .map(authService::getUserById)
                .map(user -> user.getBody())
                .collect(Collectors.toList());

        return ChatDTO.builder()
                .id(chat.getId())
                .chatName(chat.getChatName())
                .isGroup(chat.getIsGroup())
                .participants(users)
                .createdAt(chat.getCreatedAt())
                .build();
    }
}
