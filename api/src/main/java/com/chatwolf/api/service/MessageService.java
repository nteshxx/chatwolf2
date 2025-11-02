package com.chatwolf.api.service;

import com.chatwolf.api.dto.MessageDTO;
import com.chatwolf.api.dto.SendMessageRequest;
import com.chatwolf.api.entity.Message;
import com.chatwolf.api.repository.MessageRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public List<MessageDTO> getMessages(Long chatId) {
        return messageRepository.findByChatIdOrderBySentAtAsc(chatId).stream()
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId())
                        .chatId(msg.getChatId().toString())
                        .sender(authService.getUserById(msg.getSenderId()).getBody())
                        .content(msg.getContent())
                        .attachmentUrl(msg.getAttachmentUrl())
                        .sentAt(msg.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }

    public MessageDTO sendMessage(Long chatId, SendMessageRequest request) {
        Message message = Message.builder()
                .chatId(chatId)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        messageRepository.save(message);

        // Send async notification
        notificationService.sendNotification(Map.of(
                "type",
                "MESSAGE",
                "chatId",
                chatId,
                "senderId",
                request.getSenderId(),
                "content",
                request.getContent()));

        return MessageDTO.builder()
                .id(message.getId())
                .chatId(chatId.toString())
                .sender(authService.getUserById(request.getSenderId()).getBody())
                .content(message.getContent())
                .attachmentUrl(message.getAttachmentUrl())
                .sentAt(message.getSentAt())
                .build();
    }
}
