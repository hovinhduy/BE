package com.iuh.fit.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iuh.fit.inventory_service.event.ProductCreatedEvent;
import com.iuh.fit.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product-created-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductCreatedEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record.value();

            ProductCreatedEvent event;
            if (payload instanceof ProductCreatedEvent) {
                event = (ProductCreatedEvent) payload;
            } else if (payload instanceof Map) {
                // Xử lý khi payload là map từ JSON
                event = objectMapper.convertValue(payload, ProductCreatedEvent.class);
            } else {
                log.info("Loại payload nhận được: {}", payload.getClass().getName());
                log.info("Nội dung payload: {}", payload);

                // Chuyển đổi trực tiếp từ Map
                Map<String, Object> productData = (Map<String, Object>) payload;
                event = new ProductCreatedEvent();

                // Trích xuất dữ liệu từ payload
                if (productData.containsKey("id")) {
                    event.setId(Long.valueOf(productData.get("id").toString()));
                }
                if (productData.containsKey("name")) {
                    event.setName((String) productData.get("name"));
                }
                if (productData.containsKey("quantity")) {
                    event.setQuantity(Integer.valueOf(productData.get("quantity").toString()));
                }
                if (productData.containsKey("price")) {
                    event.setPrice(Double.valueOf(productData.get("price").toString()));
                }
            }

            if (event.getId() != null) {
                log.info("Nhận được ProductCreatedEvent cho sản phẩm có ID: {}", event.getId());
                inventoryService.processProductCreatedEvent(event);
            } else {
                log.warn("Nhận được ProductCreatedEvent không hợp lệ: {}", payload);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý ProductCreatedEvent: {}", e.getMessage(), e);
        }
    }
}