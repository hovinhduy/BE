package com.ktpm.productService.service;

import com.ktpm.productService.dto.event.ProductCreatedEvent;
import com.ktpm.productService.dto.response.ResultPaginationDTO;
import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.repository.CategoryRepository;
import com.ktpm.productService.repository.ManufactureRepository;
import com.ktpm.productService.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ManufactureRepository manufactureRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaProducerService kafkaProducerService;

    public ProductService(ProductRepository productRepository, ManufactureRepository manufactureRepository,
            CategoryRepository categoryRepository, KafkaProducerService kafkaProducerService) {
        this.productRepository = productRepository;
        this.manufactureRepository = manufactureRepository;
        this.categoryRepository = categoryRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public ResultPaginationDTO getAllProducts(Specification<Product> jobSpecification, Pageable pageable) {
        Page<Product> pageProduct = productRepository.findAll(jobSpecification, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageProduct.getTotalPages());
        meta.setTotal(pageProduct.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(pageProduct.getContent());

        return rs;
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

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

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
