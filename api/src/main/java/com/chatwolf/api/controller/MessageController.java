package com.chatwolf.api.controller;

import com.chatwolf.api.dto.MessageDTO;
import com.chatwolf.api.dto.SendMessageRequest;
import com.chatwolf.api.service.MessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(messageService.getMessages(chatId));
    }

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(@PathVariable Long chatId, @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(chatId, request));
    }
}
