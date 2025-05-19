package com.iuh.fit.order_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
//    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;
    
    @NotNull(message = "ID địa chỉ giao hàng không được để trống")
    private Long shippingAddressId;
    
    private Long billingAddressId;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
    
    @NotBlank(message = "Phương thức giao hàng không được để trống")
    private String shippingMethod;
    
    private BigDecimal discountAmount;
    
    private BigDecimal shippingAmount;
    
    private BigDecimal taxAmount;
    
    private String notes;
} 