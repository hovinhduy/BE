package com.ktpm.paymentService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentCallbackController {
    @PostMapping("/callback")
    public ResponseEntity<String> paymentCallback(@RequestBody Map<String, Object> payload) {
        System.out.println("🔁 Callback từ PayOS: " + payload);
        // xử lý cập nhật trạng thái đơn hàng (gọi lại order-service)
        return ResponseEntity.ok("Received");
    }
}
