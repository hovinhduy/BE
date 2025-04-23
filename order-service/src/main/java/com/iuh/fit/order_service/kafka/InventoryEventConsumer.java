package com.iuh.fit.order_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.iuh.fit.order_service.dto.InventoryUpdateEvent;
import com.iuh.fit.order_service.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderService orderService;
    
    @KafkaListener(
        topics = "${app.kafka.topic.inventory-updated}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryUpdatedEvent(
            @Payload InventoryUpdateEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Đã nhận thông báo cập nhật tồn kho: Order: {}, Success: {}", 
                event.getOrderNumber(), event.isSuccess());
        
        try {
            if (event.isSuccess()) {
                log.info("Cập nhật tồn kho thành công cho đơn hàng: {}", event.getOrderNumber());
            } else {
                log.error("Lỗi khi cập nhật tồn kho cho đơn hàng: {}, Lỗi: {}", 
                        event.getOrderNumber(), event.getMessage());
                
                // Nếu thất bại, cập nhật trạng thái đơn hàng thành CANCELLED
                orderService.cancelOrder(
                        event.getOrderId(), 
                        "Hủy đơn hàng do không đủ tồn kho: " + event.getMessage(),
                        "SYSTEM_INVENTORY");
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý sự kiện cập nhật tồn kho: {}", e.getMessage(), e);
        }
    }
} 