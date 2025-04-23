package com.iuh.fit.inventory_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateEvent {
    private Long orderId;
    private String orderNumber;
    private Long productId;
    private Integer quantity;
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
}