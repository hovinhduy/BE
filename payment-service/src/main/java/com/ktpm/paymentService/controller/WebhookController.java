package com.ktpm.paymentService.controller;

import com.ktpm.paymentService.dto.PaymentConfirmationEvent;
import com.ktpm.paymentService.dto.WebhookResponse;
import com.ktpm.paymentService.dto.WebhookTestRequest;
import com.ktpm.paymentService.dto.WebhookVerifyData;
import com.ktpm.paymentService.service.PaymentKafkaProducer;
import com.ktpm.paymentService.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final WebhookService webhookService;
    private final PaymentKafkaProducer paymentKafkaProducer;

    public WebhookController(WebhookService webhookService, PaymentKafkaProducer paymentKafkaProducer) {
        this.webhookService = webhookService;
        this.paymentKafkaProducer = paymentKafkaProducer;
    }

    /**
     * API xác thực và đăng ký webhook URL
     * 
     * @param request Yêu cầu chứa webhook URL cần đăng ký
     * @return Kết quả xác thực và đăng ký
     */
    @PostMapping("/confirm-webhook")
    public ResponseEntity<WebhookResponse> confirmWebhook(
            @RequestHeader("x-client-id") String clientId,
            @RequestHeader("x-api-key") String apiKey,
            @RequestBody WebhookTestRequest request) {

        log.info("Nhận yêu cầu xác thực webhook URL: {}", request.getWebhookUrl());

        // Thực hiện xác thực và đăng ký webhook
        WebhookResponse response = webhookService.verifyAndRegisterWebhook(request.getWebhookUrl());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // Xác định mã status HTTP dựa trên mã lỗi
            HttpStatus status = switch (response.getCode()) {
                case "400" -> HttpStatus.BAD_REQUEST;
                case "401" -> HttpStatus.UNAUTHORIZED;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };

            return ResponseEntity.status(status).body(response);
        }
    }

    /**
     * API để xác thực webhook - được gọi bởi PayOS để kiểm tra kết nối
     * 
     * @param payload Dữ liệu test từ PayOS
     * @return Kết quả xác thực
     */
    @PostMapping("/callback")
    public ResponseEntity<Object> handleWebhookTest(
            @RequestBody Map<String, Object> payload) {

        log.info("Nhận yêu cầu kiểm tra webhook từ PayOS: {}", payload);

        try {
            // Trích xuất dữ liệu và chữ ký từ payload
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String signature = (String) payload.get("signature");

            if (data == null || signature == null) {
                log.error("Payload không hợp lệ, thiếu trường data hoặc signature");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "code", "400",
                                "desc", "Payload không hợp lệ, thiếu trường data hoặc signature",
                                "success", false));
            }

            // Chuyển đổi data thành WebhookVerifyData
            WebhookVerifyData verifyData = WebhookVerifyData.builder()
                    .orderCode((Integer) data.get("orderCode"))
                    .amount((Integer) data.get("amount"))
                    .description((String) data.get("description"))
                    .accountNumber((String) data.get("accountNumber"))
                    .reference((String) data.get("reference"))
                    .transactionDateTime((String) data.get("transactionDateTime"))
                    .currency((String) data.get("currency"))
                    .paymentLinkId((String) data.get("paymentLinkId"))
                    .code((String) data.get("code"))
                    .desc((String) data.get("desc"))
                    .counterAccountBankId((String) data.get("counterAccountBankId"))
                    .counterAccountBankName((String) data.get("counterAccountBankName"))
                    .counterAccountName((String) data.get("counterAccountName"))
                    .counterAccountNumber((String) data.get("counterAccountNumber"))
                    .virtualAccountName((String) data.get("virtualAccountName"))
                    .virtualAccountNumber((String) data.get("virtualAccountNumber"))
                    .build();

            // Xác thực chữ ký
            boolean isValid = webhookService.verifyWebhookData(verifyData, signature);

            if (isValid) {
                log.info("Xác thực webhook thành công");

                // Chuyển tiếp thông báo đến order-service qua Kafka - Chỉ nếu data.code == "00"
                if ("00".equals(data.get("code"))) {
                    try {
                        // Trích xuất orderId của order-service từ description
                        // Ví dụ: "Thanh toan don hang 1131"
                        String description = (String) data.get("description");
                        Long orderServiceId = extractOrderServiceIdFromDescription(description);

                        if (orderServiceId != null) {
                            PaymentConfirmationEvent event = PaymentConfirmationEvent.builder()
                                    .orderServiceId(orderServiceId)
                                    .paymentData(data) // data từ webhook
                                    .signature(signature)
                                    .build();
                            paymentKafkaProducer.sendPaymentConfirmation(event);
                        } else {
                            log.error("Không thể trích xuất orderServiceId từ description: '{}'", description);
                            // Cân nhắc: có nên trả lỗi cho PayOS nếu không parse được ID?
                            // Hiện tại: không trả lỗi, để payment-service vẫn nhận là thành công.
                        }

                    } catch (Exception e) {
                        log.error("Lỗi khi gửi sự kiện xác nhận thanh toán qua Kafka: " + e.getMessage(), e);
                        // Không trả về lỗi cho PayOS nếu gặp vấn đề với Kafka
                    }
                }

                // Trả về kết quả thành công theo định dạng yêu cầu của PayOS
                return ResponseEntity.ok(Map.of(
                        "code", "00",
                        "desc", "success"));
            } else {
                log.error("Xác thực webhook thất bại: chữ ký không hợp lệ");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "code", "400",
                                "desc", "Chữ ký không hợp lệ"));
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "code", "500",
                            "desc", "Lỗi khi xử lý webhook: " + e.getMessage()));
        }
    }

    // Helper method to extract order service ID from description
    private Long extractOrderServiceIdFromDescription(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        // Ví dụ: "Thanh toan don hang 1131"
        // Cần một logic parse ổn định hơn nếu format description có thể thay đổi.
        try {
            String[] parts = description.split(" ");
            if (parts.length > 0) {
                // Tìm phần tử cuối cùng là số
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (parts[i].matches("\\d+")) { // Kiểm tra nếu là số
                        return Long.parseLong(parts[i]);
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Không thể parse Long từ description: '{}'", description, e);
        }
        log.warn("Không tìm thấy ID đơn hàng dạng số trong description: '{}'", description);
        return null;
    }
}