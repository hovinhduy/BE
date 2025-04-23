package com.iuh.fit.inventory_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.iuh.fit.inventory_service.dto.OrderCreatedEvent;
import com.iuh.fit.inventory_service.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "${app.kafka.topic.order-created}", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Đã nhận thông báo tạo đơn hàng mới: {}", event.getOrderNumber());
        try {
            inventoryService.processOrderCreatedEvent(event);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý sự kiện đơn hàng mới: {}", e.getMessage(), e);
        }
    }
}