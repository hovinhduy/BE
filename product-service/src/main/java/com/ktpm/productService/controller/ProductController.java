package com.ktpm.productService.controller;

import com.ktpm.productService.model.Product;
import com.ktpm.productService.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
         this.productService = productService;
    }

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/product/{id}")
    public Product getProductById(Long id) {
        return productService.getProductById(id);
    }

    @PostMapping("/product")
    public Product addProduct(Product product) {
        return productService.saveProduct(product);
    }

    @PutMapping("/product")
    public Product updateProduct(Product product) {
        return productService.saveProduct(product);
    }

    @DeleteMapping("/product/{id}")
    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }
}
