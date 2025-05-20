package com.iuh.fit.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Long id;
    private String name;
    private Double price; // Giá từ product-service là Double
    private String image; // Sử dụng trường này để lấy URL hình ảnh
} 