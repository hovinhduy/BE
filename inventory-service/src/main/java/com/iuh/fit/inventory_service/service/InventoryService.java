package com.iuh.fit.inventory_service.service;

import java.util.List;

import com.iuh.fit.inventory_service.dto.InventoryCheckRequest;
import com.iuh.fit.inventory_service.dto.InventoryCheckResponse;
import com.iuh.fit.inventory_service.dto.InventoryDTO;
import com.iuh.fit.inventory_service.dto.OrderCreatedEvent;
import com.iuh.fit.inventory_service.dto.UpdateInventoryRequest;
import com.iuh.fit.inventory_service.dto.RestoreInventoryRequest;
import com.iuh.fit.inventory_service.event.ProductCreatedEvent;

public interface InventoryService {

    InventoryDTO getInventoryByProductId(Long productId);

    InventoryDTO updateInventory(Long productId, UpdateInventoryRequest request);

    /**
     * Kiểm tra xem số lượng sản phẩm có đủ không
     */
    InventoryCheckResponse checkInventory(InventoryCheckRequest request);

    /**
     * Kiểm tra danh sách sản phẩm có đủ số lượng không
     */
    List<InventoryCheckResponse> checkInventoryBatch(List<InventoryCheckRequest> requests);

    /**
     * Lấy danh sách tồn kho theo danh sách ID sản phẩm.
     */
    List<InventoryDTO> getInventoriesByProductIds(List<Long> productIds);

    /**
     * Xử lý khi nhận được thông báo đơn hàng mới
     */
    void processOrderCreatedEvent(OrderCreatedEvent event);

    /**
     * Xử lý khi nhận được thông báo sản phẩm mới được tạo
     */
    void processProductCreatedEvent(ProductCreatedEvent event);

    /**
     * Giảm số lượng tồn kho dựa trên thông tin đơn hàng
     */
    boolean reduceInventory(Long productId, Integer quantity, Long orderId, String orderNumber);

    /**
     * Xóa tồn kho theo ID sản phẩm.
     */
    void deleteInventoryByProductId(Long productId);

    /**
     * Hoàn lại số lượng sản phẩm trong kho khi đơn hàng bị hủy
     */
    List<InventoryDTO> restoreInventory(List<RestoreInventoryRequest> requests);
}