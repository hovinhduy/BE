package com.iuh.fit.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.iuh.fit.order_service.entity.Order;
import com.iuh.fit.order_service.entity.OrderHistory;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    
    List<OrderHistory> findByOrderId(Long orderId);
    
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.order.id = ?1 ORDER BY oh.createdAt DESC")
    List<OrderHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    List<OrderHistory> findByOrderOrderByCreatedAtDesc(Order order);
} 