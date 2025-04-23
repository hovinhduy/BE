package com.iuh.fit.order_service.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.CartItemDTO;
import com.iuh.fit.order_service.entity.Cart;
import com.iuh.fit.order_service.entity.CartItem;

@Component
public class CartMapper {

    public CartDTO toDTO(Cart cart) {
        if (cart == null) {
            return null;
        }
        
        CartDTO cartDTO = new CartDTO();
        cartDTO.setId(cart.getId());
        cartDTO.setUserId(cart.getUserId());
        cartDTO.setCreatedAt(cart.getCreatedAt());
        cartDTO.setUpdatedAt(cart.getUpdatedAt());
        
        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(this::toCartItemDTO)
                .collect(Collectors.toList());
        
        cartDTO.setItems(itemDTOs);
        
        return cartDTO;
    }
    
    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        
        CartItemDTO cartItemDTO = new CartItemDTO();
        cartItemDTO.setId(cartItem.getId());
        cartItemDTO.setProductId(cartItem.getProductId());
        cartItemDTO.setQuantity(cartItem.getQuantity());
        cartItemDTO.setPrice(cartItem.getPrice());
        
        return cartItemDTO;
    }
    
    public CartItem toCartItem(CartItemDTO cartItemDTO) {
        if (cartItemDTO == null) {
            return null;
        }
        
        CartItem cartItem = new CartItem();
        cartItem.setId(cartItemDTO.getId());
        cartItem.setProductId(cartItemDTO.getProductId());
        cartItem.setQuantity(cartItemDTO.getQuantity());
        cartItem.setPrice(cartItemDTO.getPrice());
        
        return cartItem;
    }
} 