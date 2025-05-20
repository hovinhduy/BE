package com.iuh.fit.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.iuh.fit.order_service.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    private Long id;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private String shippingAddress;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items = new ArrayList<>();
} 