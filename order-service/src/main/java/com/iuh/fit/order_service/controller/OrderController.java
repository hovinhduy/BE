package com.iuh.fit.order_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iuh.fit.order_service.dto.ApiResponse;
import com.iuh.fit.order_service.dto.CreateOrderRequest;
import com.iuh.fit.order_service.dto.OrderDTO;
import com.iuh.fit.order_service.dto.OrderHistoryDTO;
import com.iuh.fit.order_service.entity.OrderStatus;
import com.iuh.fit.order_service.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Đơn hàng", description = "API quản lý đơn hàng")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới từ giỏ hàng hiện tại")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Tạo đơn hàng mới cho người dùng: {}", request.getUserId());
        OrderDTO order = orderService.createOrder(request);
        return new ResponseEntity<>(ApiResponse.success("Tạo đơn hàng thành công", order), HttpStatus.CREATED);
    }
    
    @GetMapping
    @Operation(summary = "Lấy danh sách đơn hàng của người dùng")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrdersByUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        log.info("Lấy danh sách đơn hàng của người dùng: {}", userId);
        Page<OrderDTO> orders = orderService.getOrdersByUserId(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết đơn hàng theo ID")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        log.info("Lấy chi tiết đơn hàng: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", order));
    }
    
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(defaultValue = "SYSTEM") String cancelledBy) {
        
        log.info("Hủy đơn hàng: {}", id);
        OrderDTO order = orderService.cancelOrder(id, reason, cancelledBy);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", order));
    }
    
    @PostMapping("/{id}/pay")
    @Operation(summary = "Xác nhận thanh toán đơn hàng")
    public ResponseEntity<ApiResponse<OrderDTO>> processPayment(
            @PathVariable Long id,
            @RequestParam String paymentDetails) {
        
        log.info("Xác nhận thanh toán đơn hàng: {}", id);
        OrderDTO order = orderService.processPayment(id, paymentDetails);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công", order));
    }
    
    @PostMapping("/{id}/refund")
    @Operation(summary = "Yêu cầu hoàn tiền đơn hàng")
    public ResponseEntity<ApiResponse<OrderDTO>> processRefund(
            @PathVariable Long id,
            @RequestParam String refundDetails,
            @RequestParam(defaultValue = "SYSTEM") String refundedBy) {
        
        log.info("Xử lý hoàn tiền đơn hàng: {}", id);
        OrderDTO order = orderService.processRefund(id, refundDetails, refundedBy);
        return ResponseEntity.ok(ApiResponse.success("Xử lý hoàn tiền thành công", order));
    }
    
    @GetMapping("/{id}/history")
    @Operation(summary = "Lấy lịch sử đơn hàng")
    public ResponseEntity<ApiResponse<List<OrderHistoryDTO>>> getOrderHistory(@PathVariable Long id) {
        log.info("Lấy lịch sử đơn hàng: {}", id);
        List<OrderHistoryDTO> history = orderService.getOrderHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công", history));
    }
    
    @GetMapping("/status")
    @Operation(summary = "Lấy danh sách đơn hàng theo trạng thái")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByStatus(
            @RequestParam Long userId,
            @RequestParam OrderStatus status) {
        
        log.info("Lấy danh sách đơn hàng của người dùng {} theo trạng thái: {}", userId, status);
        List<OrderDTO> orders = orderService.getOrdersByUserIdAndStatus(userId, status);
        
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
    }
} 