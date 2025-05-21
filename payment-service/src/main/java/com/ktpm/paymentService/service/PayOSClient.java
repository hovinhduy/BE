package com.ktpm.paymentService.service;

import com.ktpm.paymentService.config.PayOSConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class PayOSClient {
    private final PayOSConfig config;

    public PayOSClient(PayOSConfig config) {
        this.config = config;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateSignature(Integer orderCode, Integer amount, String cancelUrl, String returnUrl,
            String description) {
        String rawData = String.format(
                "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                amount, cancelUrl, description, orderCode, returnUrl);

        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(config.getChecksumKey().getBytes(), "HmacSHA256");
            hmacSha256.init(secretKey);

            byte[] hash = hmacSha256.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký HMAC_SHA256", e);
        }
    }

    public String createPaymentUrl(Long orderId, int orderCode, Double amount) {
        String cancelUrl = "http://localhost:4200/PaymentCancel";
        String returnUrl = "http://localhost:4200/PaymentSuccess";
        String description = "Thanh toán đơn hàng #" + orderId;

        String signature = generateSignature(orderCode, amount.intValue(), cancelUrl, returnUrl, description);

        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount.intValue());
        body.put("description", "Thanh toán đơn hàng #" + orderId);
        //Thời gian hết hạn của link thanh toán, là Unix Timestamp và kiểu Int32
        int expiredAt = (int) (System.currentTimeMillis() / 1000) + 300;
        body.put("expiredAt", expiredAt);
        body.put("returnUrl", returnUrl);
        body.put("cancelUrl", cancelUrl);
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key", config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api-merchant.payos.vn/v2/payment-requests", request, Map.class);

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null || responseBody.get("data") == null) {
            throw new RuntimeException("Không nhận được phản hồi hợp lệ từ PayOS: " + responseBody);
        }

        Map<?, ?> data = (Map<?, ?>) responseBody.get("data");

        if (data.get("checkoutUrl") == null) {
            throw new RuntimeException("Không có checkoutUrl trong phản hồi: " + data);
        }
        System.out.println("PayOS response: " + response.getBody());
        return data.get("checkoutUrl").toString();
    }

    public String getPaymentStatus(int orderCode) {

        String url = "https://api-merchant.payos.vn/v2/payment-requests/" + orderCode;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key", config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null || responseBody.get("data") == null) {
            throw new RuntimeException("Không nhận được phản hồi hợp lệ từ PayOS: " + responseBody);
        }

        Map<?, ?> data = (Map<?, ?>) responseBody.get("data");

        Object status = data.get("status");
        if (status == null) {
            throw new RuntimeException("Không có trạng thái (status) trong phản hồi: " + data);
        }

        System.out.println("PayOS status response: " + data);
        return status.toString(); // Ví dụ: PAID, PENDING, CANCELLED
    }
}
