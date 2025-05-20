package com.iuh.fit.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreInventoryRequest {
    private Long productId;
    private Integer quantity;
    private String orderNumber;
} 