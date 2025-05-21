package com.ktpm.paymentService.service;

import com.ktpm.paymentService.dto.PaymentConfirmationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentConfirmationEvent> kafkaTemplate;

    @Value("${app.kafka.topic.payment-confirmed:payment-confirmed-topic}")
    private String paymentConfirmedTopic;

    public void sendPaymentConfirmation(PaymentConfirmationEvent event) {
        try {
            log.info("Gửi sự kiện xác nhận thanh toán đến Kafka topic {}: {}", paymentConfirmedTopic, event);
            kafkaTemplate.send(paymentConfirmedTopic, String.valueOf(event.getOrderServiceId()), event);
            log.info("Đã gửi thành công sự kiện xác nhận thanh toán cho orderId {} lên topic {}",
                    event.getOrderServiceId(), paymentConfirmedTopic);
        } catch (Exception e) {
            log.error("Lỗi khi gửi sự kiện xác nhận thanh toán cho orderId {} lên topic {}: {}",
                    event.getOrderServiceId(), paymentConfirmedTopic, e.getMessage(), e);
        }
    }
}