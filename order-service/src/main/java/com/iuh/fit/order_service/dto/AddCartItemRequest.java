package com.iuh.fit.order_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;
    
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity = 1;
    
    @NotNull(message = "Giá sản phẩm không được để trống")
    private BigDecimal price;
    
    private String productName;
    
    private String productImage;
} 