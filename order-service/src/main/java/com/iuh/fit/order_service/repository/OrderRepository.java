package com.iuh.fit.order_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iuh.fit.order_service.entity.Order;
import com.iuh.fit.order_service.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    Optional<Order> findByOrderNumber(String orderNumber);
} 