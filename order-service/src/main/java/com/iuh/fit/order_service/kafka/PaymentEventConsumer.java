package com.iuh.fit.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iuh.fit.order_service.dto.OrderDTO;
import com.iuh.fit.order_service.dto.PaymentConfirmationEvent;
import com.iuh.fit.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "${app.kafka.topic.payment-confirmed:payment-confirmed-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentConfirmedEvent(ConsumerRecord<String, Object> record) {
        log.info("Đã nhận thông điệp từ Kafka topic '{}' với key '{}'", 
                record.topic(), record.key());
        
        try {
            Object message = record.value();
            PaymentConfirmationEvent event;
            
            if (message instanceof PaymentConfirmationEvent) {
                event = (PaymentConfirmationEvent) message;
            } else if (message instanceof LinkedHashMap) {
                // Xử lý trường hợp message là LinkedHashMap
                log.info("Message nhận được dưới dạng LinkedHashMap, đang chuyển đổi thành PaymentConfirmationEvent");
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) message;
                
                event = new PaymentConfirmationEvent();
                
                // Trích xuất orderServiceId
                if (map.containsKey("orderServiceId")) {
                    Object value = map.get("orderServiceId");
                    if (value instanceof Number) {
                        event.setOrderServiceId(((Number) value).longValue());
                    } else if (value instanceof String) {
                        event.setOrderServiceId(Long.parseLong((String) value));
                    }
                }
                
                // Trích xuất paymentData
                if (map.containsKey("paymentData") && map.get("paymentData") instanceof Map) {
                    event.setPaymentData((Map<String, Object>) map.get("paymentData"));
                }
                
                // Trích xuất signature
                if (map.containsKey("signature")) {
                    event.setSignature((String) map.get("signature"));
                }
            } else {
                // Nếu không phải PaymentConfirmationEvent hoặc LinkedHashMap, 
                // thử chuyển đổi từ JSON thông qua ObjectMapper
                try {
                    log.info("Thử chuyển đổi message kiểu {} thành PaymentConfirmationEvent qua ObjectMapper", 
                            message.getClass().getName());
                    event = objectMapper.convertValue(message, PaymentConfirmationEvent.class);
                    log.info("Chuyển đổi thành công qua ObjectMapper");
                } catch (Exception e) {
                    log.error("Không thể chuyển đổi message thông qua ObjectMapper: {}", e.getMessage());
                    return;
                }
            }
            
            log.info("Đã nhận sự kiện xác nhận thanh toán từ Kafka: Đơn hàng ID {}", event.getOrderServiceId());

            Map<String, Object> paymentData = event.getPaymentData();
            Long orderId = event.getOrderServiceId();

            if (orderId == null) {
                log.error("Sự kiện xác nhận thanh toán không có orderServiceId. Key: {}", record.key());
                return;
            }

            if (paymentData == null) {
                log.error("Sự kiện xác nhận thanh toán không có paymentData. Key: {}", record.key());
                return;
            }

            // Kiểm tra trạng thái thanh toán từ paymentData (tương tự như trong webhook cũ)
            boolean isSuccess = paymentData.containsKey("code") && "00".equals(paymentData.get("code"));

            if (isSuccess) {
                // Xây dựng chuỗi paymentDetails
                String paymentDetails = String.format(
                        "Thanh toán qua PayOS: %s, SĐT: %s, Người thanh toán: %s, Số tiền: %s, Mô tả: %s",
                        paymentData.getOrDefault("reference", "N/A"),
                        paymentData.getOrDefault("accountNumber", "N/A"),
                        paymentData.getOrDefault("counterAccountName", "N/A"),
                        paymentData.getOrDefault("amount", "N/A"),
                        paymentData.getOrDefault("description", "N/A")
                );

                OrderDTO updatedOrder = orderService.processPayment(orderId, paymentDetails);
                log.info("Đã cập nhật thành công trạng thái đơn hàng {} sau khi nhận sự kiện thanh toán.", updatedOrder.getId());
            } else {
                log.warn("Sự kiện xác nhận thanh toán cho đơn hàng {} không thành công. Mã lỗi: {}. Mô tả: {}", 
                        orderId, paymentData.get("code"), paymentData.get("desc"));
                // Có thể cần xử lý thêm nếu thanh toán không thành công, ví dụ: hủy đơn hàng
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý sự kiện xác nhận thanh toán: {}. Record: {}", e.getMessage(), record, e);
            // Xử lý lỗi, ví dụ: gửi vào dead-letter topic
        }
    }
} 