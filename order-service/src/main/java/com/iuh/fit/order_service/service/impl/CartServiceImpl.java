package com.iuh.fit.order_service.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iuh.fit.order_service.dto.AddCartItemRequest;
import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.UpdateCartItemRequest;
import com.iuh.fit.order_service.entity.Cart;
import com.iuh.fit.order_service.entity.CartItem;
import com.iuh.fit.order_service.exception.ResourceNotFoundException;
import com.iuh.fit.order_service.mapper.CartMapper;
import com.iuh.fit.order_service.repository.CartItemRepository;
import com.iuh.fit.order_service.repository.CartRepository;
import com.iuh.fit.order_service.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addItemToCart(AddCartItemRequest request) {
        Cart cart = getOrCreateCart(request.getUserId());
        
        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();
        
        if (existingItem.isPresent()) {
            // Nếu đã có, cập nhật số lượng
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
        } else {
            // Nếu chưa có, thêm mới
            CartItem cartItem = new CartItem();
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(request.getPrice());
            cart.addCartItem(cartItem);
        }
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        
        return cartMapper.toDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateCartItem(Long itemId, UpdateCartItemRequest request) {
        Cart cart = getCartEntityByUserId(request.getUserId());
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
        
        // Kiểm tra xem cart item có thuộc giỏ hàng của user không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("CartItem không thuộc giỏ hàng của người dùng này");
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        
        return cartMapper.toDTO(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long itemId, Long userId) {
        Cart cart = getCartEntityByUserId(userId);
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
        
        // Kiểm tra xem cart item có thuộc giỏ hàng của user không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("CartItem không thuộc giỏ hàng của người dùng này");
        }
        
        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);
        
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartEntityByUserId(userId);
        
        cart.getCartItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        
        log.info("Đã xóa toàn bộ giỏ hàng của người dùng: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Cart getCartEntityByUserId(Long userId) {
        return getOrCreateCart(userId);
    }
    
    // Helper method
    private Cart getOrCreateCart(Long userId) {
        Optional<Cart> existingCart = cartRepository.findByUserId(userId);
        
        if (existingCart.isPresent()) {
            return existingCart.get();
        } else {
            // Tạo giỏ hàng mới nếu chưa có
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            LocalDateTime now = LocalDateTime.now();
            newCart.setCreatedAt(now);
            newCart.setUpdatedAt(now);
            
            return cartRepository.save(newCart);
        }
    }
} 