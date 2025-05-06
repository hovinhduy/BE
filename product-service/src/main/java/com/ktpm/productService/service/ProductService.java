package com.ktpm.productService.service;

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

    public ProductService(ProductRepository productRepository, ManufactureRepository manufactureRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.manufactureRepository = manufactureRepository;
        this.categoryRepository = categoryRepository;
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
        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        Product oldProduct = productRepository.findById(product.getId()).orElse(null);
        Optional.ofNullable(product.getName()).ifPresent(oldProduct::setName);
        Optional.ofNullable(product.getPrice()).ifPresent(oldProduct::setPrice);
        Optional.ofNullable(product.getImage()).ifPresent(oldProduct::setImage);
        Optional.ofNullable(product.getDetailDesc()).ifPresent(oldProduct::setDetailDesc);
        Optional.ofNullable(product.getQuantity()).ifPresent(oldProduct::setQuantity);
        Optional.ofNullable(product.getShortDesc()).ifPresent(oldProduct::setShortDesc);
        Optional.ofNullable(product.getSoid()).ifPresent(oldProduct::setSoid);
        if (product.getName() != null) oldProduct.setName(product.getName());
        if (product.getCategory() != null){
            oldProduct.setCategory(categoryRepository.findById(product.getCategory().getId()).orElse(null));
        }

        if (product.getManufacture() != null){
            oldProduct.setManufacture(manufactureRepository.findById(product.getManufacture().getId()).orElse(null));
        }
        return productRepository.save(oldProduct);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
