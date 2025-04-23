package com.iuh.fit.order_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.iuh.fit.order_service.dto.InventoryCheckRequest;
import com.iuh.fit.order_service.dto.InventoryCheckResponse;
import com.iuh.fit.order_service.entity.CartItem;
import com.iuh.fit.order_service.exception.ServiceUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;
    
    @Value("${app.inventory-service.url}")
    private String inventoryServiceUrl;
    
    /**
     * Kiểm tra tồn kho cho một sản phẩm
     */
    public InventoryCheckResponse checkInventory(Long productId, Integer quantity) {
        try {
            InventoryCheckRequest request = new InventoryCheckRequest(productId, quantity);
            
            ResponseEntity<ApiResponse<InventoryCheckResponse>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/check",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<InventoryCheckResponse>>() {});
            
            return response.getBody().getData();
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra tồn kho cho sản phẩm ID: " + productId, e);
            throw new ServiceUnavailableException("Không thể kết nối đến inventory-service: " + e.getMessage());
        }
    }
    
    /**
     * Kiểm tra tồn kho cho nhiều sản phẩm
     */
    public List<InventoryCheckResponse> checkInventoryBatch(List<CartItem> cartItems) {
        try {
            List<InventoryCheckRequest> requests = new ArrayList<>();
            
            for (CartItem item : cartItems) {
                requests.add(new InventoryCheckRequest(item.getProductId(), item.getQuantity()));
            }
            
            ResponseEntity<ApiResponse<List<InventoryCheckResponse>>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/check-batch",
                    HttpMethod.POST,
                    new HttpEntity<>(requests),
                    new ParameterizedTypeReference<ApiResponse<List<InventoryCheckResponse>>>() {});
            
            return response.getBody().getData();
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra tồn kho hàng loạt", e);
            throw new ServiceUnavailableException("Không thể kết nối đến inventory-service: " + e.getMessage());
        }
    }
    
    /**
     * Lớp data wrapper cho API response
     */
    @lombok.Data
    public static class ApiResponse<T> {
        private int status;
        private String message;
        private T data;
    }
} 