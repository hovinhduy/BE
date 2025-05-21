package com.iuh.fit.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmationEvent {
    private Long orderServiceId; // ID đơn hàng từ order-service
    private Map<String, Object> paymentData; // Dữ liệu 'data' từ webhook của PayOS
    private String signature; // Chữ ký từ webhook của PayOS (có thể không cần thiết ở consumer)
} 