package com.ktpm.productService.client;

import com.ktpm.productService.dto.client.ApiResponse;
import com.ktpm.productService.dto.client.InventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryServiceClient {

    @GetMapping("/batch")
    ApiResponse<List<InventoryDTO>> getInventoriesByProductIds(@RequestParam("productIds") List<Long> productIds);
}