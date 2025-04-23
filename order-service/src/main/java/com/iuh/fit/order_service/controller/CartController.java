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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iuh.fit.order_service.dto.AddCartItemRequest;
import com.iuh.fit.order_service.dto.ApiResponse;
import com.iuh.fit.order_service.dto.CartDTO;
import com.iuh.fit.order_service.dto.UpdateCartItemRequest;
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
    
    @GetMapping
    @Operation(summary = "Lấy thông tin giỏ hàng của người dùng")
    public ResponseEntity<ApiResponse<CartDTO>> getCart(@RequestParam Long userId) {
        log.info("Lấy thông tin giỏ hàng của người dùng: {}", userId);
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cart));
    }
    
    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<CartDTO>> addItemToCart(@Valid @RequestBody AddCartItemRequest request) {
        log.info("Thêm sản phẩm {} vào giỏ hàng của người dùng {}", request.getProductId(), request.getUserId());
        CartDTO cart = cartService.addItemToCart(request);
        return new ResponseEntity<>(ApiResponse.success("Thêm sản phẩm vào giỏ hàng thành công", cart), HttpStatus.CREATED);
    }
    
    @PutMapping("/items/{id}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Cập nhật số lượng sản phẩm trong giỏ hàng: {}", id);
        CartDTO cart = cartService.updateCartItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giỏ hàng thành công", cart));
    }
    
    @DeleteMapping("/items/{id}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable Long id,
            @RequestParam Long userId) {
        log.info("Xóa sản phẩm {} khỏi giỏ hàng của người dùng {}", id, userId);
        cartService.removeCartItem(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }
    
    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart(@RequestParam Long userId) {
        log.info("Xóa toàn bộ giỏ hàng của người dùng: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa giỏ hàng thành công", null));
    }
}