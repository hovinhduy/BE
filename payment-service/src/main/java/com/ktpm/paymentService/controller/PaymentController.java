package com.ktpm.paymentService.controller;

import com.ktpm.paymentService.dto.PaymentRequest;
import com.ktpm.paymentService.model.Payment;
import com.ktpm.paymentService.model.PaymentStatus;
import com.ktpm.paymentService.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final RestTemplate restTemplate;

    public PaymentController(PaymentService paymentService, RestTemplate restTemplate) {
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

        // if (updated.getStatus() == PaymentStatus.SUCCESS) {
        // restTemplate.postForEntity(orderCallbackUrl, Map.of(
        // "orderId", orderId,
        // "status", "PAID"
        // ), Void.class);
        // }

        return ResponseEntity.ok("Update status successfully");
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<String> handleCheckStatus(
            @PathVariable("id") Integer orderCode,
            HttpServletRequest servletRequest
    ) {
        if (orderCode == null) {
            return ResponseEntity.badRequest().body("orderCode is null");
        }

        Long orderId = paymentService.getOrderIdByOrderCode(orderCode);
        String status = paymentService.checkStatus(orderId);

        if ("CANCELLED".equals(status)) {
            // Lấy token từ header Authorization
            String token = servletRequest.getHeader("Authorization");

            // Tạo URL có chứa query params
            String url = UriComponentsBuilder
                    .fromHttpUrl(orderCallbackUrl + "/api/orders/" + orderId + "/cancel")
                    .queryParam("reason", "Thanh toán đã bị hủy")
                    .toUriString();

            // Tạo headers và gắn token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token); // token đã bao gồm "Bearer ..."
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok(status);
    }

}
