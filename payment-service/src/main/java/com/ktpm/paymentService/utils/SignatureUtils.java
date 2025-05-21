package com.ktpm.paymentService.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureUtils {

    private final ObjectMapper objectMapper;

    /**
     * Kiểm tra tính hợp lệ của chữ ký dựa trên dữ liệu và checksumKey
     *
     * @param data        Dữ liệu gốc
     * @param signature   Chữ ký cần kiểm tra
     * @param checksumKey Khóa để tạo HMAC
     * @return true nếu chữ ký khớp, ngược lại là false
     */
    public boolean isValidSignature(Object data, String signature, String checksumKey) {
        try {
            // Chuyển đổi dữ liệu thành Map
            Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);

            // Sắp xếp các key theo thứ tự bảng chữ cái
            String sortedParams = dataMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            log.debug("Sorted parameters for signature: {}", sortedParams);

            // Tạo HMAC-SHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(sortedParams.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi byte array thành chuỗi hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();
            log.debug("Calculated signature: {}", calculatedSignature);
            log.debug("Provided signature:  {}", signature);

            // So sánh chữ ký
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error validating signature", e);
            return false;
        }
    }
}