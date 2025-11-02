package com.chatwolf.api.controller;

import com.chatwolf.api.dto.ChatDTO;
import com.chatwolf.api.dto.CreateChatRequest;
import com.chatwolf.api.service.ChatService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getUserChats(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(chatService.getUserChats(userId));
    }

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(@RequestBody CreateChatRequest req) {
        return ResponseEntity.ok(chatService.createChat(req));
    }

    // for testing purpose only
    @GetMapping("/admin/data")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> getAdminData(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("message", "Admin only data", "user", jwt.getSubject());
    }

    // for testing purpose only
    @GetMapping("/user/data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Map<String, String> getUserData(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("message", "User data", "user", jwt.getSubject());
    }
}
