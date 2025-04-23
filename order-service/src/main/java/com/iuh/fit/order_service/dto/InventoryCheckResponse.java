package com.iuh.fit.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResponse {
    private Long productId;
    private String productName;
    private boolean isAvailable;
    private Integer availableQuantity;
    private Integer requestedQuantity;
    private String message;
} 