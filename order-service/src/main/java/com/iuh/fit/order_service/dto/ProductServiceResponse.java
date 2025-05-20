package com.iuh.fit.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductServiceResponse<T> {
    private int statusCode;
    private String error;
    private Object message; // Thông điệp có thể là String hoặc List<String>
    private T data;
} 