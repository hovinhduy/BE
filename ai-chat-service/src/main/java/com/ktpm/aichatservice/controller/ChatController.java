package com.ktpm.aichatservice.controller;

import com.ktpm.aichatservice.dto.ChatRequest;
import com.ktpm.aichatservice.entity.ChatMessage;
import com.ktpm.aichatservice.service.GeminiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final GeminiChatService chatService;

    public ChatController(GeminiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatMessage> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request.getUserId(), request.getMessage()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getHistory(userId));
    }
}
