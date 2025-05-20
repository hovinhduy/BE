package com.iuh.fit.inventory_service.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iuh.fit.inventory_service.dto.InventoryCheckRequest;
import com.iuh.fit.inventory_service.dto.InventoryCheckResponse;
import com.iuh.fit.inventory_service.dto.InventoryDTO;
import com.iuh.fit.inventory_service.dto.InventoryUpdateEvent;
import com.iuh.fit.inventory_service.dto.OrderCreatedEvent;
import com.iuh.fit.inventory_service.dto.RestoreInventoryRequest;
import com.iuh.fit.inventory_service.dto.UpdateInventoryRequest;
import com.iuh.fit.inventory_service.entity.Inventory;
import com.iuh.fit.inventory_service.exception.NotFoundException;
import com.iuh.fit.inventory_service.repository.InventoryRepository;
import com.iuh.fit.inventory_service.service.InventoryService;
import com.iuh.fit.inventory_service.event.ProductCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

        private final InventoryRepository inventoryRepository;
        private final KafkaTemplate<String, InventoryUpdateEvent> kafkaTemplate;

        @Value("${app.kafka.topic.inventory-updated}")
        private String inventoryUpdatedTopic;

        @Override
        public InventoryDTO getInventoryByProductId(Long productId) {
                log.info("Tìm tồn kho cho sản phẩm với ID: {}", productId);

                Inventory inventory = inventoryRepository.findByProductId(productId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy tồn kho cho sản phẩm có ID: " + productId));

                return mapToDTO(inventory);
        }

        @Override
        @Transactional
        public InventoryDTO updateInventory(Long productId, UpdateInventoryRequest request) {
                log.info("Cập nhật tồn kho cho sản phẩm với ID: {}", productId);

                Inventory inventory = inventoryRepository.findByProductId(productId)
                                .orElseGet(() -> {
                                        log.info("Không tìm thấy sản phẩm có ID: {}, sẽ tạo mới", productId);
                                        return new Inventory(null, productId, 0, null);
                                });

                inventory.setQuantity(request.getQuantity());

                if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
                        inventory.setProductName(request.getProductName());
                }

                Inventory savedInventory = inventoryRepository.save(inventory);
                log.info("Đã cập nhật/tạo mới thành công tồn kho cho sản phẩm: {}", savedInventory.getProductId());

                return mapToDTO(savedInventory);
        }

        @Override
        public InventoryCheckResponse checkInventory(InventoryCheckRequest request) {
                log.info("Kiểm tra tồn kho cho sản phẩm ID: {}, số lượng yêu cầu: {}",
                                request.getProductId(), request.getRequestedQuantity());

                Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                                .orElse(null);

                if (inventory == null) {
                        return InventoryCheckResponse.builder()
                                        .productId(request.getProductId())
                                        .isAvailable(false)
                                        .availableQuantity(0)
                                        .requestedQuantity(request.getRequestedQuantity())
                                        .message("Sản phẩm không tồn tại trong hệ thống")
                                        .build();
                }

                boolean isAvailable = inventory.getQuantity() >= request.getRequestedQuantity();

                return InventoryCheckResponse.builder()
                                .productId(request.getProductId())
                                .productName(inventory.getProductName())
                                .isAvailable(isAvailable)
                                .availableQuantity(inventory.getQuantity())
                                .requestedQuantity(request.getRequestedQuantity())
                                .message(isAvailable ? "Có đủ số lượng trong kho" : "Không đủ số lượng trong kho")
                                .build();
        }

        @Override
        public List<InventoryCheckResponse> checkInventoryBatch(List<InventoryCheckRequest> requests) {
                log.info("Kiểm tra tồn kho cho {} sản phẩm", requests.size());

                return requests.stream()
                                .map(this::checkInventory)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void processOrderCreatedEvent(OrderCreatedEvent event) {
                log.info("Xử lý sự kiện đơn hàng mới được tạo: Order ID: {}, Order Number: {}",
                                event.getOrderId(), event.getOrderNumber());

                List<String> errors = new ArrayList<>();

                // Kiểm tra xem tất cả sản phẩm có đủ tồn kho không
                for (OrderCreatedEvent.OrderItemDto item : event.getOrderItems()) {
                        InventoryCheckResponse checkResponse = checkInventory(
                                        new InventoryCheckRequest(item.getProductId(), item.getQuantity()));

                        if (!checkResponse.isAvailable()) {
                                errors.add(String.format("Sản phẩm ID: %d - %s: %s",
                                                item.getProductId(),
                                                checkResponse.getProductName(),
                                                checkResponse.getMessage()));
                        }
                }

                // Nếu có lỗi, không cập nhật tồn kho và gửi thông báo lỗi
                if (!errors.isEmpty()) {
                        log.error("Không thể xử lý đơn hàng do thiếu tồn kho: {}", String.join(", ", errors));

                        // Gửi thông báo lỗi qua Kafka
                        InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                        .orderId(event.getOrderId())
                                        .orderNumber(event.getOrderNumber())
                                        .success(false)
                                        .message("Không thể hoàn thành đơn hàng do thiếu tồn kho: "
                                                        + String.join(", ", errors))
                                        .timestamp(LocalDateTime.now())
                                        .build();

                        kafkaTemplate.send(inventoryUpdatedTopic, event.getOrderNumber(), updateEvent);
                        return;
                }

                // Nếu đủ tồn kho cho tất cả sản phẩm, tiến hành cập nhật tồn kho
                for (OrderCreatedEvent.OrderItemDto item : event.getOrderItems()) {
                        boolean success = reduceInventory(
                                        item.getProductId(),
                                        item.getQuantity(),
                                        event.getOrderId(),
                                        event.getOrderNumber());

                        if (!success) {
                                log.error("Lỗi khi giảm tồn kho cho sản phẩm ID: {}", item.getProductId());
                        }
                }

                // Gửi thông báo thành công qua Kafka
                InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                .orderId(event.getOrderId())
                                .orderNumber(event.getOrderNumber())
                                .success(true)
                                .message("Đã cập nhật tồn kho thành công cho đơn hàng")
                                .timestamp(LocalDateTime.now())
                                .build();

                kafkaTemplate.send(inventoryUpdatedTopic, event.getOrderNumber(), updateEvent);
        }

        @Override
        @Transactional
        public boolean reduceInventory(Long productId, Integer quantity, Long orderId, String orderNumber) {
                log.info("Giảm tồn kho: Product ID: {}, Số lượng: {}, Order: {}",
                                productId, quantity, orderNumber);

                try {
                        Inventory inventory = inventoryRepository.findByProductId(productId)
                                        .orElseThrow(() -> new NotFoundException(
                                                        "Không tìm thấy tồn kho cho sản phẩm ID: " + productId));

                        if (inventory.getQuantity() < quantity) {
                                log.error("Không đủ tồn kho cho sản phẩm ID: {}. Hiện có: {}, Yêu cầu: {}",
                                                productId, inventory.getQuantity(), quantity);

                                // Gửi thông báo lỗi qua Kafka
                                InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                                .orderId(orderId)
                                                .orderNumber(orderNumber)
                                                .productId(productId)
                                                .quantity(quantity)
                                                .success(false)
                                                .message("Không đủ tồn kho cho sản phẩm ID: " + productId)
                                                .timestamp(LocalDateTime.now())
                                                .build();

                                kafkaTemplate.send(inventoryUpdatedTopic, orderNumber, updateEvent);
                                return false;
                        }

                        // Giảm số lượng tồn kho
                        inventory.setQuantity(inventory.getQuantity() - quantity);
                        inventoryRepository.save(inventory);

                        log.info("Đã giảm tồn kho thành công: Product ID: {}, Số lượng còn lại: {}",
                                        productId, inventory.getQuantity());

                        // Gửi thông báo thành công qua Kafka
                        InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                        .orderId(orderId)
                                        .orderNumber(orderNumber)
                                        .productId(productId)
                                        .quantity(quantity)
                                        .success(true)
                                        .message("Đã giảm tồn kho thành công")
                                        .timestamp(LocalDateTime.now())
                                        .build();

                        kafkaTemplate.send(inventoryUpdatedTopic, orderNumber, updateEvent);
                        return true;

                } catch (Exception e) {
                        log.error("Lỗi khi giảm tồn kho cho sản phẩm ID: " + productId, e);

                        // Gửi thông báo lỗi qua Kafka
                        InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                        .orderId(orderId)
                                        .orderNumber(orderNumber)
                                        .productId(productId)
                                        .quantity(quantity)
                                        .success(false)
                                        .message("Lỗi khi giảm tồn kho: " + e.getMessage())
                                        .timestamp(LocalDateTime.now())
                                        .build();

                        kafkaTemplate.send(inventoryUpdatedTopic, orderNumber, updateEvent);
                        return false;
                }
        }

        @Override
        public void processProductCreatedEvent(ProductCreatedEvent event) {
                log.info("Đang xử lý sự kiện ProductCreatedEvent cho sản phẩm ID: {}", event.getId());

                // Kiểm tra xem sản phẩm đã tồn tại trong inventory chưa
                Optional<Inventory> existingInventory = inventoryRepository.findByProductId(event.getId());

                if (existingInventory.isPresent()) {
                        log.info("Sản phẩm ID: {} đã tồn tại trong inventory", event.getId());
                        return;
                }

                // Tạo mới inventory với thông tin từ event
                Inventory inventory = new Inventory();
                inventory.setProductId(event.getId());
                inventory.setProductName(event.getName());
                inventory.setQuantity(event.getQuantity());

                inventoryRepository.save(inventory);
                log.info("Đã tạo inventory mới cho sản phẩm ID: {}", event.getId());
        }

        @Override
        public List<InventoryDTO> getInventoriesByProductIds(List<Long> productIds) {
                log.info("Lấy thông tin tồn kho cho danh sách productIds: {}", productIds);
                List<Inventory> inventories = inventoryRepository.findAllByProductIdIn(productIds);
                return inventories.stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void deleteInventoryByProductId(Long productId) {
                log.info("Xóa tồn kho cho sản phẩm với ID: {}", productId);
                Optional<Inventory> inventoryOptional = inventoryRepository.findByProductId(productId);
                if (inventoryOptional.isPresent()) {
                        inventoryRepository.deleteByProductId(productId);
                        log.info("Đã xóa thành công tồn kho cho sản phẩm ID: {}", productId);
                } else {
                        log.warn("Không tìm thấy tồn kho để xóa cho sản phẩm ID: {}", productId);
                        // Optionally, you could throw a NotFoundException here if strictness is
                        // required
                        // throw new NotFoundException("Không tìm thấy tồn kho cho sản phẩm có ID: " +
                        // productId);
                }
        }

        @Override
        @Transactional
        public List<InventoryDTO> restoreInventory(List<RestoreInventoryRequest> requests) {
                log.info("Hoàn lại số lượng sản phẩm trong kho cho {} sản phẩm", requests.size());

                List<InventoryDTO> updatedInventories = new ArrayList<>();

                for (RestoreInventoryRequest request : requests) {
                        try {
                                Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                                                .orElseThrow(() -> new NotFoundException(
                                                                "Không tìm thấy tồn kho cho sản phẩm ID: "
                                                                                + request.getProductId()));

                                // Tăng số lượng tồn kho
                                inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
                                inventoryRepository.save(inventory);

                                log.info("Đã hoàn lại tồn kho thành công: Product ID: {}, Số lượng hoàn lại: {}, Số lượng mới: {}",
                                                request.getProductId(), request.getQuantity(), inventory.getQuantity());

                                // Gửi thông báo cập nhật tồn kho qua Kafka nếu có orderNumber
                                if (request.getOrderNumber() != null && !request.getOrderNumber().isEmpty()) {
                                        InventoryUpdateEvent updateEvent = InventoryUpdateEvent.builder()
                                                        .productId(request.getProductId())
                                                        .quantity(request.getQuantity())
                                                        .orderNumber(request.getOrderNumber())
                                                        .success(true)
                                                        .message("Đã hoàn lại tồn kho thành công khi hủy đơn hàng")
                                                        .timestamp(LocalDateTime.now())
                                                        .build();

                                        kafkaTemplate.send(inventoryUpdatedTopic, request.getOrderNumber(),
                                                        updateEvent);
                                }

                                updatedInventories.add(mapToDTO(inventory));
                        } catch (Exception e) {
                                log.error("Lỗi khi hoàn lại tồn kho cho sản phẩm ID: " + request.getProductId(), e);
                                // Tiếp tục xử lý các sản phẩm khác
                        }
                }

                return updatedInventories;
        }

        private InventoryDTO mapToDTO(Inventory inventory) {
                return InventoryDTO.builder()
                                .productId(inventory.getProductId())
                                .quantity(inventory.getQuantity())
                                .productName(inventory.getProductName())
                                .build();
        }
}