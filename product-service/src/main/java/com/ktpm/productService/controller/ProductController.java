package com.ktpm.productService.controller;

import com.ktpm.productService.dto.response.RestResponse;
import com.ktpm.productService.dto.response.ResultPaginationDTO;
import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.service.CategoryService;
import com.ktpm.productService.service.ManufactureService;
import com.ktpm.productService.service.ProductService;
import com.ktpm.productService.utils.annotation.ApiMessage;
import com.ktpm.productService.utils.error.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ManufactureService manufactureService;

    public ProductController(ProductService productService, CategoryService categoryService, ManufactureService manufactureService) {
         this.productService = productService;
         this.categoryService = categoryService;
         this.manufactureService = manufactureService;
    }

    @GetMapping("/product")
    @ApiMessage("Get all products")
    public ResponseEntity<ResultPaginationDTO> getAllProducts(
            @Filter Specification<Product> spec, Pageable pageable
    ) {
        ResponseEntity<ResultPaginationDTO> a = ResponseEntity.status(HttpStatus.OK).body(productService.getAllProducts(spec, pageable));
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProducts(spec, pageable));
    }

    @GetMapping("/product/{id}")
    @ApiMessage("Get product by id")
    public ResponseEntity<Product> getProductById(@PathVariable("id") String id) throws IdInvalidException {
        Product prouduct = productService.getProductById(id);
        if (prouduct == null) {
            throw new IdInvalidException("Product with id = " + id + " not found");
        }
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping("/product")
    @ApiMessage("Add new product")
    public ResponseEntity<Product> addProduct(@RequestBody Product product) throws IdInvalidException {
        Category category = product.getCategory();
        if(category == null) {
            throw new IdInvalidException("Category ID is required");
        }else {
            if(categoryService.getCategoryById(category.getId()) == null) {
                throw new IdInvalidException("Category with id = " + category.getId() + " not found");
            }
            product.setCategory(categoryService.getCategoryById(category.getId()));
        }
        Manufacture manufacture = product.getManufacture();
        if(manufacture == null) {
            throw new IdInvalidException("Manufacture ID is required");
        }else {
            if(manufactureService.getManufactureById(manufacture.getId()) == null) {
                throw new IdInvalidException("Manufacture with id = " + manufacture.getId() + " not found");
            }
            product.setManufacture(manufactureService.getManufactureById(manufacture.getId()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(product));
    }

    @PutMapping("/product")
    @ApiMessage("Update product")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product) throws IdInvalidException {
        if(product.getId() == null) {
            throw new IdInvalidException("Product ID is required");
        }
        if(productService.getProductById(product.getId()) == null) {
            throw new IdInvalidException("Product with id = " + product.getId() + " not found");
        }
        if(product.getManufacture() != null) {
            String id = product.getManufacture().getId();
            if(manufactureService.getManufactureById(id) == null) {
                throw new IdInvalidException("Manufacture with id = " + id + " not found");
            }
        }
        if(product.getCategory() != null) {
            String id = product.getCategory().getId();
            if(categoryService.getCategoryById(id) == null) {
                throw new IdInvalidException("Category with id = " + id + " not found");
            }
        }
        return ResponseEntity.ok(productService.updateProduct(product));
    }

    @DeleteMapping("/product/{id}")
    @ApiMessage("Delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") String id) throws IdInvalidException {
        if(productService.getProductById(id) == null) {
            throw new IdInvalidException("Product with id = " + id + " not found");
        }
        productService.deleteProduct(id);
        return ResponseEntity.ok().body(null);
    }
}
