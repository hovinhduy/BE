package com.ktpm.paymentService.controller;

import com.ktpm.paymentService.dto.PaymentRequest;
import com.ktpm.paymentService.model.Payment;
import com.ktpm.paymentService.model.PaymentStatus;
import com.ktpm.paymentService.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final RestTemplate restTemplate;

    public PaymentController(PaymentService paymentService, RestTemplate restTemplate){
        this.paymentService = paymentService;
        this.restTemplate = restTemplate;
    }

    @Value("${order.service.url}")
    private String orderCallbackUrl;

    @PostMapping("/payos")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody PaymentRequest request) {
        String checkoutUrl = paymentService.createPaymentAndGetUrl(request);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    @PostMapping("/status")
    public ResponseEntity<String> handleCallback(@RequestBody Map<String, Object> payload) {
        int orderCode = Integer.parseInt(payload.get("orderCode").toString());
        String status = payload.get("status").toString(); // PAID or CANCELLED

        Payment updated = paymentService.updateStatus(orderCode, status);

//        if (updated.getStatus() == PaymentStatus.SUCCESS) {
//            restTemplate.postForEntity(orderCallbackUrl, Map.of(
//                    "orderId", orderId,
//                    "status", "PAID"
//            ), Void.class);
//        }

        return ResponseEntity.ok("Update status successfully");
    }
}
