package com.ktpm.aichatservice.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long userId;
    private String message;
}
