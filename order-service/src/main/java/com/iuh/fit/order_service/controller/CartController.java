package com.iuh.fit.order_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iuh.fit.order_service.dto.AddCartItemRequest;
import com.iuh.fit.order_service.dto.ApiResponse;
import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.UpdateCartItemRequest;
import com.iuh.fit.order_service.security.SecurityUtils;
import com.iuh.fit.order_service.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Giỏ hàng", description = "API quản lý giỏ hàng")
public class CartController {
    
    private final CartService cartService;
    private final SecurityUtils securityUtils;
    
    @GetMapping
    @Operation(summary = "Lấy thông tin giỏ hàng của người dùng")
    public ResponseEntity<ApiResponse<CartDTO>> getCart() {
        Long userId = securityUtils.getCurrentUserId();
        log.info("Lấy thông tin giỏ hàng của người dùng: {}", userId);
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cart));
    }
    
    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<CartDTO>> addItemToCart(@Valid @RequestBody AddCartItemRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        
        // Gán userId từ token vào request
        request.setUserId(userId);
        
        log.info("Thêm sản phẩm {} vào giỏ hàng của người dùng {}", request.getProductId(), userId);
        CartDTO cart = cartService.addItemToCart(request);
        return new ResponseEntity<>(ApiResponse.success("Thêm sản phẩm vào giỏ hàng thành công", cart), HttpStatus.CREATED);
    }
    
    @PutMapping("/items/{productId}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItem(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        
        // Gán userId từ token vào request
        request.setUserId(userId);
        
        log.info("Cập nhật số lượng sản phẩm {} trong giỏ hàng của người dùng {}", productId, userId);
        CartDTO cart = cartService.updateCartItemByProductId(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giỏ hàng thành công", cart));
    }
    
    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable String productId) {
        Long userId = securityUtils.getCurrentUserId();
        
        log.info("Xóa sản phẩm {} khỏi giỏ hàng của người dùng {}", productId, userId);
        cartService.removeCartItemByProductId(productId, userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }
    
    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        
        log.info("Xóa toàn bộ giỏ hàng của người dùng: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa giỏ hàng thành công", null));
    }
}