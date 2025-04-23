package com.iuh.fit.order_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iuh.fit.order_service.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    void deleteByCartIdAndProductId(Long cartId, Long productId);
} 