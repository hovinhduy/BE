package com.iuh.fit.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.iuh.fit.inventory_service.dto.OrderCreatedEvent;
import com.iuh.fit.inventory_service.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topic.order-created}", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCreatedEvent(ConsumerRecord<String, Object> record) {
        log.info("Đã nhận thông báo từ Kafka với key: {}", record.key());
        try {
            Object payload = record.value();

            OrderCreatedEvent event;
            if (payload instanceof OrderCreatedEvent) {
                event = (OrderCreatedEvent) payload;
            } else if (payload instanceof Map) {
                // Xử lý khi payload là map từ JSON
                try {
                    event = objectMapper.convertValue(payload, OrderCreatedEvent.class);
                } catch (Exception e) {
                    log.info("Không thể chuyển đổi trực tiếp, thực hiện chuyển đổi thủ công");

                    // Chuyển đổi thủ công từ Map
                    Map<String, Object> orderData = (Map<String, Object>) payload;
                    event = new OrderCreatedEvent();

                    // Trích xuất dữ liệu từ payload
                    if (orderData.containsKey("orderId")) {
                        event.setOrderId(Long.valueOf(orderData.get("orderId").toString()));
                    }

                    if (orderData.containsKey("orderNumber")) {
                        event.setOrderNumber((String) orderData.get("orderNumber"));
                    }

                    if (orderData.containsKey("createdAt")) {
                        Object createdAtObj = orderData.get("createdAt");
                        if (createdAtObj instanceof List) {
                            List<?> dateValues = (List<?>) createdAtObj;
                            if (dateValues.size() >= 6) {
                                int year = Integer.parseInt(dateValues.get(0).toString());
                                int month = Integer.parseInt(dateValues.get(1).toString());
                                int day = Integer.parseInt(dateValues.get(2).toString());
                                int hour = Integer.parseInt(dateValues.get(3).toString());
                                int minute = Integer.parseInt(dateValues.get(4).toString());
                                int second = Integer.parseInt(dateValues.get(5).toString());

                                event.setCreatedAt(LocalDateTime.of(year, month, day, hour, minute, second));
                            }
                        }
                    }

                    if (orderData.containsKey("orderItems") && orderData.get("orderItems") instanceof List) {
                        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) orderData.get("orderItems");
                        List<OrderCreatedEvent.OrderItemDto> orderItems = new ArrayList<>();

                        for (Map<String, Object> itemData : itemsData) {
                            OrderCreatedEvent.OrderItemDto itemDto = new OrderCreatedEvent.OrderItemDto();

                            if (itemData.containsKey("productId")) {
                                itemDto.setProductId(Long.valueOf(itemData.get("productId").toString()));
                            }

                            if (itemData.containsKey("quantity")) {
                                itemDto.setQuantity(Integer.valueOf(itemData.get("quantity").toString()));
                            }

                            orderItems.add(itemDto);
                        }

                        event.setOrderItems(orderItems);
                    }
                }
            } else {
                log.error("Loại payload không được hỗ trợ: {}", payload.getClass().getName());
                return;
            }

            log.info("Đã nhận thông báo tạo đơn hàng mới: {}", event.getOrderNumber());
            inventoryService.processOrderCreatedEvent(event);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý sự kiện đơn hàng mới: {}", e.getMessage(), e);
        }
    }
}