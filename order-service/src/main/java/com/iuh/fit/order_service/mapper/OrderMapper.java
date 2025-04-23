package com.iuh.fit.order_service.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.iuh.fit.order_service.dto.OrderDTO;
import com.iuh.fit.order_service.dto.OrderHistoryDTO;
import com.iuh.fit.order_service.dto.OrderItemDTO;
import com.iuh.fit.order_service.entity.Order;
import com.iuh.fit.order_service.entity.OrderHistory;
import com.iuh.fit.order_service.entity.OrderItem;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }
        
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setOrderNumber(order.getOrderNumber());
        orderDTO.setUserId(order.getUserId());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setDiscountAmount(order.getDiscountAmount());
        orderDTO.setShippingAmount(order.getShippingAmount());
        orderDTO.setTaxAmount(order.getTaxAmount());
        orderDTO.setFinalAmount(order.getFinalAmount());
        orderDTO.setShippingAddressId(order.getShippingAddressId());
        orderDTO.setBillingAddressId(order.getBillingAddressId());
        orderDTO.setPaymentMethod(order.getPaymentMethod());
        orderDTO.setShippingMethod(order.getShippingMethod());
        orderDTO.setNotes(order.getNotes());
        orderDTO.setCreatedAt(order.getCreatedAt());
        orderDTO.setUpdatedAt(order.getUpdatedAt());
        
        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(this::toOrderItemDTO)
                .collect(Collectors.toList());
        
        orderDTO.setItems(itemDTOs);
        
        return orderDTO;
    }
    
    public OrderItemDTO toOrderItemDTO(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        
        OrderItemDTO orderItemDTO = new OrderItemDTO();
        orderItemDTO.setId(orderItem.getId());
        orderItemDTO.setProductId(orderItem.getProductId());
        orderItemDTO.setProductName(orderItem.getProductName());
        orderItemDTO.setQuantity(orderItem.getQuantity());
        orderItemDTO.setUnitPrice(orderItem.getUnitPrice());
        orderItemDTO.setTotalPrice(orderItem.getTotalPrice());
        
        return orderItemDTO;
    }
    
    public OrderHistoryDTO toOrderHistoryDTO(OrderHistory orderHistory) {
        if (orderHistory == null) {
            return null;
        }
        
        OrderHistoryDTO orderHistoryDTO = new OrderHistoryDTO();
        orderHistoryDTO.setId(orderHistory.getId());
        orderHistoryDTO.setOrderId(orderHistory.getOrder().getId());
        orderHistoryDTO.setStatus(orderHistory.getStatus());
        orderHistoryDTO.setComment(orderHistory.getComment());
        orderHistoryDTO.setCreatedBy(orderHistory.getCreatedBy());
        orderHistoryDTO.setCreatedAt(orderHistory.getCreatedAt());
        
        return orderHistoryDTO;
    }
    
    public List<OrderDTO> toDTOList(List<Order> orders) {
        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
} 