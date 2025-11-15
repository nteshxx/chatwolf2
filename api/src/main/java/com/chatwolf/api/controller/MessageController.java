package com.chatwolf.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatwolf.api.dto.ConversationSummary;
import com.chatwolf.api.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ConversationSummary>> getMessages(@PathVariable String userId) {
        return ResponseEntity.ok(messageService.getUserConversations(userId));
    }
    
}
