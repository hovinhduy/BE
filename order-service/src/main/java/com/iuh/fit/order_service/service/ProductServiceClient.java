package com.iuh.fit.order_service.service;

import com.iuh.fit.order_service.dto.ProductDetailDTO;
import com.iuh.fit.order_service.dto.ProductServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Service name "product-service" phải khớp với tên đăng ký trên Eureka
@FeignClient(name = "product-service", path = "/api") 
public interface ProductServiceClient {

    // Endpoint "/product/{productId}" trong product-service
    @GetMapping("/product/{productId}")
    ProductServiceResponse<ProductDetailDTO> getProductById(@PathVariable("productId") Long productId);
} 