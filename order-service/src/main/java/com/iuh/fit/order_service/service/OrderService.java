package com.iuh.fit.order_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.iuh.fit.order_service.dto.CreateOrderRequest;
import com.iuh.fit.order_service.dto.OrderDTO;
import com.iuh.fit.order_service.dto.OrderHistoryDTO;
import com.iuh.fit.order_service.entity.OrderStatus;

public interface OrderService {
    
    OrderDTO createOrder(CreateOrderRequest request);
    
    OrderDTO getOrderById(Long id);
    
    OrderDTO getOrderByOrderNumber(String orderNumber);
    
    Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable);
    
    List<OrderDTO> getOrdersByUserIdAndStatus(Long userId, OrderStatus status);
    
    OrderDTO updateOrderStatus(Long id, OrderStatus status, String comment, String updatedBy);
    
    OrderDTO cancelOrder(Long id, String reason, String cancelledBy);
    
    OrderDTO processPayment(Long id, String paymentDetails);
    
    OrderDTO processRefund(Long id, String refundDetails, String refundedBy);
    
    List<OrderHistoryDTO> getOrderHistory(Long orderId);
} 