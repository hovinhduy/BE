package com.iuh.fit.order_service.dto;

import java.time.LocalDateTime;

import com.iuh.fit.order_service.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryDTO {
    
    private Long id;
    private Long orderId;
    private OrderStatus status;
    private String comment;
    private String createdBy;
    private LocalDateTime createdAt;
} 