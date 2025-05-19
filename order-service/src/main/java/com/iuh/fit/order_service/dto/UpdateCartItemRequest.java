package com.iuh.fit.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    
//    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;
    
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
} 