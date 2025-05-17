//package com.iuh.fit.inventory_service.config;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.iuh.fit.inventory_service.entity.Inventory;
//import com.iuh.fit.inventory_service.repository.InventoryRepository;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Configuration
//@Slf4j
//public class DataLoader {
//
//    @Bean
//    public CommandLineRunner initDatabase(InventoryRepository inventoryRepository) {
//        return args -> {
//            log.info("Tạo dữ liệu mẫu cho inventory");
//
//            if (inventoryRepository.count() == 0) {
//                inventoryRepository.save(new Inventory(null, 1L, 100, "iPhone 15 Pro Max"));
//                inventoryRepository.save(new Inventory(null, 2L, 50, "Samsung Galaxy S24 Ultra"));
//                inventoryRepository.save(new Inventory(null, 3L, 75, "Xiaomi 14 Pro"));
//                inventoryRepository.save(new Inventory(null, 4L, 30, "OPPO Find X7 Ultra"));
//
//                log.info("Đã tạo dữ liệu mẫu thành công");
//            }
//        };
//    }
//}