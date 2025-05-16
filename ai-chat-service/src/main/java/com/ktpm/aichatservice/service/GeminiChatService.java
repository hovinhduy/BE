package com.ktpm.aichatservice.service;

import com.ktpm.aichatservice.config.GeminiConfig;
import com.ktpm.aichatservice.entity.ChatMessage;
import com.ktpm.aichatservice.repository.ChatMessageRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiChatService {
    private final GeminiConfig config;
    private final ChatMessageRepository chatRepo;
    private final RestTemplate restTemplate;

    public GeminiChatService(GeminiConfig config, ChatMessageRepository chatRepo, RestTemplate restTemplate){
        this.config = config;
        this.chatRepo = chatRepo;
        this.restTemplate = restTemplate;
    }

    public ChatMessage chat(Long userId, String prompt) {
        // 1. Lưu tin nhắn người dùng
        chatRepo.save(new ChatMessage(null, userId, "user", prompt, LocalDateTime.now()));

        // 2. Tạo request cho Gemini API
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));

        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fullUrl = config.getApiUrl() + "?key=" + config.getApiKey();

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                fullUrl, HttpMethod.POST, request, Map.class
        );

        // 3. Xử lý phản hồi từ Gemini
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("candidates")) {
            throw new RuntimeException("Phản hồi từ Gemini không hợp lệ");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        Map<String, Object> contentObj = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
        String aiReply = parts.get(0).get("text").toString();

        // 4. Lưu phản hồi của AI
//        chatRepo.save(new ChatMessage(null, userId, "assistant", aiReply, LocalDateTime.now()));

        // 5. Trả lại lịch sử
        return chatRepo.save(new ChatMessage(null, userId, "assistant", aiReply, LocalDateTime.now()));
    }

    public List<ChatMessage> getHistory(Long userId) {
        return chatRepo.findByUserIdOrderByTimestampAsc(userId);
    }
}
