package com.iuh.fit.inventory_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

import com.iuh.fit.inventory_service.dto.ApiResponse;
import com.iuh.fit.inventory_service.dto.InventoryCheckRequest;
import com.iuh.fit.inventory_service.dto.InventoryCheckResponse;
import com.iuh.fit.inventory_service.dto.InventoryDTO;
import com.iuh.fit.inventory_service.dto.UpdateInventoryRequest;
import com.iuh.fit.inventory_service.repository.InventoryRepository;
import com.iuh.fit.inventory_service.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

        private final InventoryService inventoryService;
        private final InventoryRepository inventoryRepository;

        private static final String ADMIN_ROLE = "ADMIN";

        @GetMapping("/{productId}")
        public ResponseEntity<ApiResponse<InventoryDTO>> getInventory(@PathVariable Long productId) {
                log.info("Nhận yêu cầu lấy thông tin tồn kho cho sản phẩm có ID: {}", productId);

                InventoryDTO inventory = inventoryService.getInventoryByProductId(productId);

                ApiResponse<InventoryDTO> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                "Lấy thông tin tồn kho thành công",
                                inventory);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{productId}")
        public ResponseEntity<ApiResponse<InventoryDTO>> updateInventory(
                        @PathVariable Long productId,
                        @Valid @RequestBody UpdateInventoryRequest request,
                        @RequestHeader(value = "X-ROLES", required = false) String roles) {

                log.info("Nhận yêu cầu cập nhật tồn kho cho sản phẩm có ID: {}, bởi người dùng có vai trò: {}",
                                productId, roles);

                if (roles == null || !roles.contains(ADMIN_ROLE)) {
                        log.warn("Người dùng không có quyền ADMIN để cập nhật tồn kho cho sản phẩm ID: {}. Vai trò hiện tại: {}",
                                        productId, roles);
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập");
                }

                // Kiểm tra sản phẩm đã tồn tại chưa để xác định thông báo phù hợp
                boolean isNewProduct = !inventoryRepository.findByProductId(productId).isPresent();

                InventoryDTO updatedInventory = inventoryService.updateInventory(productId, request);

                String message = isNewProduct
                                ? "Đã tạo mới tồn kho cho sản phẩm thành công"
                                : "Cập nhật tồn kho thành công";

                ApiResponse<InventoryDTO> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                message,
                                updatedInventory);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/check")
        public ResponseEntity<ApiResponse<InventoryCheckResponse>> checkInventory(
                        @Valid @RequestBody InventoryCheckRequest request) {

                log.info("Nhận yêu cầu kiểm tra tồn kho cho sản phẩm có ID: {}", request.getProductId());

                InventoryCheckResponse checkResult = inventoryService.checkInventory(request);

                ApiResponse<InventoryCheckResponse> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                "Kiểm tra tồn kho thành công",
                                checkResult);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/check-batch")
        public ResponseEntity<ApiResponse<List<InventoryCheckResponse>>> checkInventoryBatch(
                        @Valid @RequestBody List<InventoryCheckRequest> requests) {

                log.info("Nhận yêu cầu kiểm tra tồn kho hàng loạt cho {} sản phẩm", requests.size());

                List<InventoryCheckResponse> checkResults = inventoryService.checkInventoryBatch(requests);

                ApiResponse<List<InventoryCheckResponse>> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                "Kiểm tra tồn kho hàng loạt thành công",
                                checkResults);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/batch")
        public ResponseEntity<ApiResponse<List<InventoryDTO>>> getInventoriesByProductIds(
                        @RequestParam("productIds") List<Long> productIds) {
                log.info("Nhận yêu cầu lấy thông tin tồn kho cho danh sách productIds: {}", productIds);
                List<InventoryDTO> inventories = inventoryService.getInventoriesByProductIds(productIds);
                ApiResponse<List<InventoryDTO>> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                "Lấy thông tin tồn kho hàng loạt thành công",
                                inventories);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{productId}")
        public ResponseEntity<ApiResponse<Void>> deleteInventoryByProductId(@PathVariable Long productId) {
                log.info("Nhận yêu cầu xóa tồn kho cho sản phẩm ID: {}", productId);
                inventoryService.deleteInventoryByProductId(productId);
                ApiResponse<Void> response = new ApiResponse<>(
                                HttpStatus.OK.value(),
                                "Xóa tồn kho cho sản phẩm ID " + productId + " thành công",
                                null);
                return ResponseEntity.ok(response);
        }
}