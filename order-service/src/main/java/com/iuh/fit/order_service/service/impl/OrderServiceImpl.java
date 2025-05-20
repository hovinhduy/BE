package com.iuh.fit.order_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iuh.fit.order_service.dto.CreateOrderRequest;
import com.iuh.fit.order_service.dto.InventoryCheckResponse;
import com.iuh.fit.order_service.dto.OrderCreatedEvent;
import com.iuh.fit.order_service.dto.OrderDTO;
import com.iuh.fit.order_service.dto.OrderHistoryDTO;
import com.iuh.fit.order_service.entity.Cart;
import com.iuh.fit.order_service.entity.CartItem;
import com.iuh.fit.order_service.entity.Order;
import com.iuh.fit.order_service.entity.OrderHistory;
import com.iuh.fit.order_service.entity.OrderItem;
import com.iuh.fit.order_service.entity.OrderStatus;
import com.iuh.fit.order_service.exception.ResourceNotFoundException;
import com.iuh.fit.order_service.mapper.OrderMapper;
import com.iuh.fit.order_service.repository.OrderHistoryRepository;
import com.iuh.fit.order_service.repository.OrderItemRepository;
import com.iuh.fit.order_service.repository.OrderRepository;
import com.iuh.fit.order_service.service.CartService;
import com.iuh.fit.order_service.service.InventoryClient;
import com.iuh.fit.order_service.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    @Value("${app.order.default-status:PENDING}")
    private String defaultOrderStatus;
    
    @Value("${app.kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Override
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Lấy giỏ hàng hiện tại của người dùng
        Cart cart = cartService.getCartEntityByUserId(request.getUserId());
        
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Không thể tạo đơn hàng với giỏ hàng trống");
        }
        
        // Kiểm tra tồn kho trước khi tạo đơn hàng
        List<InventoryCheckResponse> inventoryChecks = inventoryClient.checkInventoryBatch(cart.getCartItems());
        
        // Kiểm tra xem tất cả sản phẩm có đủ tồn kho không
        List<String> errors = new ArrayList<>();
        for (InventoryCheckResponse check : inventoryChecks) {
            if (!check.isAvailable()) {
                errors.add(String.format("Sản phẩm ID: %d - %s: %s", 
                        check.getProductId(), 
                        check.getProductName(), 
                        check.getMessage()));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Không đủ tồn kho: " + String.join(", ", errors));
        }
        
        // Tạo đơn hàng mới
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        
        // Thêm các sản phẩm từ giỏ hàng vào đơn hàng
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            // Lấy tên sản phẩm từ kết quả kiểm tra tồn kho
            InventoryCheckResponse productInfo = inventoryChecks.stream()
                    .filter(check -> check.getProductId().equals(cartItem.getProductId()))
                    .findFirst()
                    .orElse(null);
            
            orderItem.setProductName(productInfo != null && productInfo.getProductName() != null 
                    ? productInfo.getProductName() 
                    : "Product " + cartItem.getProductId()); 
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItem.calculateTotalPrice();
            
            order.addOrderItem(orderItem);
        }
        
        // Tính tổng tiền
        order.calculateTotals();
        
        // Áp dụng các khoản phí và giảm giá
        if (request.getTaxAmount() != null) {
            order.setTaxAmount(request.getTaxAmount());
        }
        
        // Tính lại tổng tiền cuối cùng
        order.calculateTotals();
        
        // Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);
        
        // Thêm lịch sử đơn hàng
        OrderHistory history = new OrderHistory();
        history.setOrder(savedOrder);
        history.setStatus(OrderStatus.PENDING);
        history.setComment("Đơn hàng mới được tạo");
        history.setCreatedBy("USER_" + request.getUserId());
        orderHistoryRepository.save(history);
        
        // Gửi thông báo qua Kafka để inventory-service cập nhật tồn kho
        sendOrderCreatedEvent(savedOrder);
        
        // Xóa giỏ hàng
        cartService.clearCart(request.getUserId());
        
        log.info("Đã tạo đơn hàng mới: {}", savedOrder.getOrderNumber());
        
        return orderMapper.toDTO(savedOrder);
    }
    
    /**
     * Gửi thông báo đơn hàng mới được tạo qua Kafka
     */
    private void sendOrderCreatedEvent(Order order) {
        try {
            List<OrderCreatedEvent.OrderItemDto> orderItems = order.getOrderItems().stream()
                    .map(item -> new OrderCreatedEvent.OrderItemDto(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList());
            
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .createdAt(order.getCreatedAt())
                    .orderItems(orderItems)
                    .build();
            
            // Sử dụng key cụ thể để đảm bảo tin nhắn được nhận theo thứ tự đúng cho từng đơn hàng
            String key = order.getOrderNumber();
            // Gửi tin nhắn với type hint để cho phép deserialize đúng
            kafkaTemplate.send(orderCreatedTopic, key, event);
            log.info("Đã gửi thông báo đơn hàng mới tạo qua Kafka: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo qua Kafka: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        
        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        List<OrderDTO> orderDTOs = orderMapper.toDTOList(orderPage.getContent());
        
        return new PageImpl<>(orderDTOs, pageable, orderPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserIdAndStatus(Long userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
        
        return orderMapper.toDTOList(orders);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, OrderStatus status, String comment, String updatedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        
        // Thêm lịch sử đơn hàng
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setComment(comment);
        history.setCreatedBy(updatedBy);
        
        orderHistoryRepository.save(history);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Đã cập nhật trạng thái đơn hàng {}: {}", id, status);
        
        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long id, String reason, String cancelledBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã giao hoặc đang giao");
        }
        
        // Hoàn lại tồn kho cho các sản phẩm trong đơn hàng
        if (order.getStatus() != OrderStatus.CANCELLED) {
            try {
                log.info("Hoàn lại tồn kho cho đơn hàng: {}", order.getOrderNumber());
                inventoryClient.restoreInventory(order.getOrderItems(), order.getOrderNumber());
                log.info("Đã hoàn lại tồn kho thành công cho đơn hàng: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi hoàn lại tồn kho cho đơn hàng: " + order.getOrderNumber(), e);
                // Vẫn tiếp tục hủy đơn hàng ngay cả khi không thể hoàn lại tồn kho
            }
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        
        // Thêm lịch sử đơn hàng
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CANCELLED);
        history.setComment("Đơn hàng đã bị hủy: " + reason);
        history.setCreatedBy(cancelledBy);
        
        orderHistoryRepository.save(history);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Đã hủy đơn hàng: {}", id);
        
        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO processPayment(Long id, String paymentDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Chỉ có thể thanh toán đơn hàng ở trạng thái PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Không thể thanh toán đơn hàng ở trạng thái: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        order.setPaymentDetails(paymentDetails);
        
        // Thêm lịch sử đơn hàng
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PAID);
        history.setComment("Đơn hàng đã được thanh toán");
        history.setCreatedBy("SYSTEM");
        
        orderHistoryRepository.save(history);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Đã xử lý thanh toán cho đơn hàng: {}", id);
        
        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO processRefund(Long id, String refundDetails, String refundedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        
        // Chỉ có thể hoàn tiền đơn hàng đã thanh toán hoặc đã giao
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Không thể hoàn tiền đơn hàng ở trạng thái: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setRefundDetails(refundDetails);
        
        // Thêm lịch sử đơn hàng
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.REFUNDED);
        history.setComment("Đơn hàng đã được hoàn tiền");
        history.setCreatedBy(refundedBy);
        
        orderHistoryRepository.save(history);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Đã xử lý hoàn tiền cho đơn hàng: {}", id);
        
        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHistoryDTO> getOrderHistory(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        
        List<OrderHistory> histories = orderHistoryRepository.findByOrderOrderByCreatedAtDesc(order);
        
        return histories.stream()
                .map(history -> OrderHistoryDTO.builder()
                        .id(history.getId())
                        .orderId(orderId)
                        .status(history.getStatus())
                        .comment(history.getComment())
                        .createdBy(history.getCreatedBy())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
} 