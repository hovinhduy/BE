package com.iuh.fit.order_service.service;

import com.iuh.fit.order_service.dto.AddCartItemRequest;
import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.UpdateCartItemRequest;
import com.iuh.fit.order_service.entity.Cart;

public interface CartService {
    
    CartDTO getCartByUserId(Long userId);
    
    CartDTO addItemToCart(AddCartItemRequest request);
    
    CartDTO updateCartItem(Long itemId, UpdateCartItemRequest request);
    
    void removeCartItem(Long itemId, Long userId);
    
    void clearCart(Long userId);
    
    Cart getCartEntityByUserId(Long userId);
} 