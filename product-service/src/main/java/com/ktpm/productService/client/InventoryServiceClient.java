package com.ktpm.productService.client;

import com.ktpm.productService.dto.client.ApiResponse;
import com.ktpm.productService.dto.client.InventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryServiceClient {

    @GetMapping("/batch")
    ApiResponse<List<InventoryDTO>> getInventoriesByProductIds(@RequestParam("productIds") List<Long> productIds);

    @GetMapping("/{productId}")
    ApiResponse<InventoryDTO> getInventoryByProductId(@PathVariable("productId") Long productId);

    @DeleteMapping("/{productId}")
    ApiResponse<Void> deleteInventoryByProductId(@PathVariable("productId") Long productId);
}