package com.ktpm.paymentService.service;

import com.ktpm.paymentService.config.PayOSConfig;
import com.ktpm.paymentService.dto.WebhookResponse;
import com.ktpm.paymentService.dto.WebhookVerifyData;
import com.ktpm.paymentService.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WebhookService {

    private final PayOSConfig payOSConfig;
    private final RestTemplate restTemplate;
    private final SignatureUtils signatureUtils;

    public WebhookService(PayOSConfig payOSConfig, RestTemplate restTemplate, SignatureUtils signatureUtils) {
        this.payOSConfig = payOSConfig;
        this.restTemplate = restTemplate;
        this.signatureUtils = signatureUtils;
    }

    /**
     * Kiểm tra URL hợp lệ
     */
    private boolean isValidUrl(String url) {
        try {
            new URI(url);
            return url.startsWith("https://");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Xác thực và đăng ký webhook URL với PayOS
     * 
     * @param webhookUrl URL webhook cần đăng ký
     * @return WebhookResponse chứa kết quả xác thực
     */
    public WebhookResponse verifyAndRegisterWebhook(String webhookUrl) {
        // Kiểm tra tính hợp lệ của URL
        if (!isValidUrl(webhookUrl)) {
            log.error("URL webhook không hợp lệ: {}. URL phải bắt đầu bằng https://", webhookUrl);
            logManualRegistrationInstructions();
            return WebhookResponse.builder()
                    .code("400")
                    .desc("Webhook URL không hợp lệ. URL phải bắt đầu bằng https://")
                    .success(false)
                    .build();
        }

        try {
            log.info("Đăng ký webhook URL với PayOS: {}", webhookUrl);

            Map<String, Object> body = new HashMap<>();
            body.put("webhookUrl", webhookUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        "https://api-merchant.payos.vn/confirm-webhook", request, Map.class);
                log.info("Kết quả đăng ký webhook: {}", response.getBody());

                // Cập nhật URL callback trong cấu hình
                payOSConfig.setCallbackUrl(webhookUrl);

                return WebhookResponse.builder()
                        .code("00")
                        .desc("success")
                        .success(true)
                        .build();
            } catch (HttpClientErrorException.BadRequest e) {
                log.error("PayOS từ chối webhook URL: {}", e.getResponseBodyAsString());
                logManualRegistrationInstructions();
                return WebhookResponse.builder()
                        .code("400")
                        .desc("Webhook URL bị từ chối bởi PayOS: " + e.getResponseBodyAsString())
                        .success(false)
                        .build();
            }
        } catch (Exception e) {
            log.error("Lỗi khi đăng ký webhook với PayOS", e);
            logManualRegistrationInstructions();
            return WebhookResponse.builder()
                    .code("500")
                    .desc("Lỗi khi đăng ký webhook: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    /**
     * Xác thực dữ liệu test webhook từ PayOS
     * 
     * @param data      Dữ liệu mẫu từ PayOS
     * @param signature Chữ ký đi kèm để xác thực
     * @return true nếu hợp lệ, ngược lại là false
     */
    public boolean verifyWebhookData(WebhookVerifyData data, String signature) {
        try {
            return signatureUtils.isValidSignature(data, signature, payOSConfig.getChecksumKey());
        } catch (Exception e) {
            log.error("Lỗi khi xác thực dữ liệu webhook", e);
            return false;
        }
    }

    /**
     * Hiển thị hướng dẫn đăng ký webhook thủ công
     */
    public void logManualRegistrationInstructions() {
        log.warn("------------------------------------------------------------");
        log.warn("KHÔNG THỂ ĐĂNG KÝ WEBHOOK TỰ ĐỘNG VỚI PAYOS");
        log.warn("Vui lòng đăng ký webhook thủ công bằng cách:");
        log.warn("1. Đăng nhập vào tài khoản PayOS tại https://my.payos.vn");
        log.warn("2. Chọn Merchant của bạn");
        log.warn("3. Vào mục Cài đặt > Cấu hình Webhook");
        log.warn("4. Nhập URL webhook: {}", payOSConfig.getCallbackUrl());
        log.warn("5. Nhấn 'Lưu' để xác nhận");
        log.warn("------------------------------------------------------------");
        log.warn("LƯU Ý: URL webhook phải:");
        log.warn("- Sử dụng HTTPS");
        log.warn("- Là domain công khai có thể truy cập từ internet");
        log.warn("- Không thể là localhost hoặc IP private");
        log.warn("- Phải phản hồi đúng định dạng khi PayOS gửi yêu cầu kiểm tra");
        log.warn("------------------------------------------------------------");
    }
}