package com.ktpm.productService.service;

import com.ktpm.productService.dto.CategoryDTO;
import com.ktpm.productService.dto.ImageDTO;
import com.ktpm.productService.dto.ManufactureDTO;
import com.ktpm.productService.dto.ProductDTO;
import com.ktpm.productService.dto.event.ProductCreatedEvent;
import com.ktpm.productService.dto.response.ResultPaginationDTO;
import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Image;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.repository.CategoryRepository;
import com.ktpm.productService.repository.ImageRepository;
import com.ktpm.productService.repository.ManufactureRepository;
import com.ktpm.productService.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

import com.ktpm.productService.client.InventoryServiceClient;
import com.ktpm.productService.dto.client.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final ManufactureRepository manufactureRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaProducerService kafkaProducerService;
    private final InventoryServiceClient inventoryServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository, ManufactureRepository manufactureRepository,
            CategoryRepository categoryRepository, KafkaProducerService kafkaProducerService,
            InventoryServiceClient inventoryServiceClient, ImageRepository imageRepository,
                          RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.manufactureRepository = manufactureRepository;
        this.categoryRepository = categoryRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.inventoryServiceClient = inventoryServiceClient;
        this.redisTemplate = redisTemplate;
    }

    public void clearProductCache() {
        redisTemplate.delete("products_0_20");
    }

    @Cacheable(value = "products", key = "'products_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public ResultPaginationDTO<ProductDTO> getAllProducts(Specification<Product> jobSpecification, Pageable pageable) {
        Page<Product> pageProduct = productRepository.findAll(jobSpecification, pageable);
        List<Product> products = pageProduct.getContent();

        if (products != null && !products.isEmpty()) {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            try {
                ApiResponse<List<com.ktpm.productService.dto.client.InventoryDTO>> inventoryResponse =
                        inventoryServiceClient.getInventoriesByProductIds(productIds);

                if (inventoryResponse != null && inventoryResponse.getData() != null) {
                    Map<Long, Integer> inventoryMap = inventoryResponse.getData().stream()
                            .collect(Collectors.toMap(com.ktpm.productService.dto.client.InventoryDTO::getProductId,
                                    com.ktpm.productService.dto.client.InventoryDTO::getQuantity,
                                    (oldValue, newValue) -> newValue));

                    products.forEach(product -> {
                        product.setQuantity(inventoryMap.getOrDefault(product.getId(), 0));
                    });
                } else {
                    log.warn("Không nhận được dữ liệu tồn kho, đặt quantity = 0");
                    products.forEach(product -> product.setQuantity(0));
                }
            } catch (Exception e) {
                log.error("Lỗi khi gọi inventory-service: {}", e.getMessage(), e);
                products.forEach(product -> product.setQuantity(0));
            }
        }

        // Ánh xạ sang DTO
        List<ProductDTO> productDTOs = products.stream().map(this::mapToDTO).toList();

        // Trả về kết quả phân trang
        ResultPaginationDTO<ProductDTO> rs = new ResultPaginationDTO<>();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageProduct.getTotalPages());
        meta.setTotal(pageProduct.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(productDTOs);

        return rs;
    }

    public Product getProductById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            try {
                ApiResponse<com.ktpm.productService.dto.client.InventoryDTO> inventoryResponse = inventoryServiceClient
                        .getInventoryByProductId(id);
                if (inventoryResponse != null && inventoryResponse.getData() != null) {
                    product.setQuantity(inventoryResponse.getData().getQuantity());
                } else {
                    log.warn(
                            "Không nhận được dữ liệu tồn kho từ inventory-service cho productId: {}. Đặt số lượng về 0.",
                            id);
                    product.setQuantity(0);
                }
            } catch (Exception e) {
                log.error("Lỗi khi gọi inventory-service để lấy số lượng cho productId: {}: {}. Đặt số lượng về 0.", id,
                        e.getMessage(), e);
                product.setQuantity(0);
            }
        }
        return product;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product saveProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        // Gửi sự kiện Kafka khi tạo sản phẩm mới
        ProductCreatedEvent event = new ProductCreatedEvent(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getQuantity(),
                savedProduct.getPrice());

        kafkaProducerService.sendProductCreatedEvent(event);

        return savedProduct;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product updateProduct(Product product) {
        Product oldProduct = productRepository.findById(product.getId()).orElse(null);
        if (product.getName() != null) {
            oldProduct.setName(product.getName());
        }
        if (product.getQuantity() != null) {
            oldProduct.setQuantity(product.getQuantity());
        }
        if (product.getShortDesc() != null) {
            oldProduct.setShortDesc(product.getShortDesc());
        }
        if (product.getDetailDesc() != null) {
            oldProduct.setDetailDesc(product.getDetailDesc());
        }
        if (product.getSold() != null) {
            oldProduct.setSold(product.getSold());
        }
        if (product.getCategory() != null) {
            oldProduct.setCategory(categoryRepository.findById(product.getCategory().getId()).orElse(null));
        }

        if (product.getManufacture() != null) {
            oldProduct.setManufacture(manufactureRepository.findById(product.getManufacture().getId()).orElse(null));
        }
        return productRepository.save(oldProduct);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        // Call inventory-service to delete inventory first
        try {
            ApiResponse<Void> response = inventoryServiceClient.deleteInventoryByProductId(id);
            if (response != null && response.getStatus() >= 200 && response.getStatus() < 300) {
                log.info("Đã xóa thành công tồn kho cho productId: {} từ inventory-service", id);
            } else {
                log.warn(
                        "Xóa tồn kho cho productId: {} từ inventory-service không thành công hoặc không có phản hồi. Status: {}, Message: {}",
                        id, response != null ? response.getStatus() : "N/A",
                        response != null ? response.getMessage() : "N/A");
                // Decide if you want to proceed with product deletion even if inventory
                // deletion fails
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi inventory-service để xóa tồn kho cho productId: {}: {}", id, e.getMessage(), e);
            // Decide if you want to proceed with product deletion even if inventory
            // deletion fails
        }
        List<Image> listImg = imageRepository.findByProductId(id);
        for (Image image : listImg) {
            imageRepository.delete(image);
        }
        productRepository.deleteById(id);
        log.info("Đã xóa sản phẩm với ID: {} từ product-service", id);
    }

    private ProductDTO mapToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setDetailDesc(product.getDetailDesc());
        dto.setShortDesc(product.getShortDesc());
        dto.setQuantity(product.getQuantity());
        dto.setSold(product.getSold());

        dto.setImages(product.getImages().stream()
                .map(img -> new ImageDTO(img.getId(), img.getUrl()))
                .collect(Collectors.toList()));

        dto.setCategory(new CategoryDTO(product.getCategory().getId(), product.getCategory().getName()));
        dto.setManufacture(new ManufactureDTO(product.getManufacture().getId(), product.getManufacture().getName()));

        return dto;
    }

    public ResultPaginationDTO<ProductDTO> getAllProductsByCategory(Category category, Specification<Product> productSpecification, Pageable pageable) {

        Page<Product> pageProduct = productRepository.findByCategory(category,productSpecification, pageable);
        List<Product> products = pageProduct.getContent();

        if (products != null && !products.isEmpty()) {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            try {
                ApiResponse<List<com.ktpm.productService.dto.client.InventoryDTO>> inventoryResponse = inventoryServiceClient
                        .getInventoriesByProductIds(productIds);

                if (inventoryResponse != null && inventoryResponse.getData() != null) {
                    Map<Long, Integer> inventoryMap = inventoryResponse.getData().stream()
                            .collect(Collectors.toMap(com.ktpm.productService.dto.client.InventoryDTO::getProductId,
                                    com.ktpm.productService.dto.client.InventoryDTO::getQuantity,
                                    (oldValue, newValue) -> newValue));

                    products.forEach(product -> {
                        product.setQuantity(inventoryMap.getOrDefault(product.getId(), 0));
                    });
                } else {
                    log.warn(
                            "Không nhận được dữ liệu tồn kho từ inventory-service hoặc dữ liệu rỗng cho productIds: {}. Đặt số lượng về 0.",
                            productIds);
                    products.forEach(product -> product.setQuantity(0));
                }
            } catch (Exception e) {
                log.error("Lỗi khi gọi inventory-service để lấy số lượng cho productIds: {}: {}. Đặt số lượng về 0.",
                        productIds, e.getMessage(), e);
                products.forEach(product -> product.setQuantity(0));
            }
        }

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageProduct.getTotalPages());
        meta.setTotal(pageProduct.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(products);

        return rs;
    }

    public ResultPaginationDTO<ProductDTO> getAllProductsByManufacture(Manufacture manufacture, Specification<Product> productSpecification, Pageable pageable) {
        Page<Product> pageProduct = productRepository.findByManufacture(manufacture, productSpecification, pageable);
        List<Product> products = pageProduct.getContent();

        if (products != null && !products.isEmpty()) {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            try {
                ApiResponse<List<com.ktpm.productService.dto.client.InventoryDTO>> inventoryResponse = inventoryServiceClient
                        .getInventoriesByProductIds(productIds);

                if (inventoryResponse != null && inventoryResponse.getData() != null) {
                    Map<Long, Integer> inventoryMap = inventoryResponse.getData().stream()
                            .collect(Collectors.toMap(com.ktpm.productService.dto.client.InventoryDTO::getProductId,
                                    com.ktpm.productService.dto.client.InventoryDTO::getQuantity,
                                    (oldValue, newValue) -> newValue));

                    products.forEach(product -> {
                        product.setQuantity(inventoryMap.getOrDefault(product.getId(), 0));
                    });
                } else {
                    log.warn(
                            "Không nhận được dữ liệu tồn kho từ inventory-service hoặc dữ liệu rỗng cho productIds: {}. Đặt số lượng về 0.",
                            productIds);
                    products.forEach(product -> product.setQuantity(0));
                }
            } catch (Exception e) {
                log.error("Lỗi khi gọi inventory-service để lấy số lượng cho productIds: {}: {}. Đặt số lượng về 0.",
                        productIds, e.getMessage(), e);
                products.forEach(product -> product.setQuantity(0));
            }
        }

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageProduct.getTotalPages());
        meta.setTotal(pageProduct.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(products);

        return rs;
    }
}
