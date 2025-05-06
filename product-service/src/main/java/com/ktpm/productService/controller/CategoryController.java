package com.ktpm.productService.controller;

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

@RequestMapping("/api")
@RestController
public class CategoryController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ManufactureService manufactureService;

    public CategoryController(ProductService productService, CategoryService categoryService, ManufactureService manufactureService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.manufactureService = manufactureService;
    }

    @GetMapping("/category")
    @ApiMessage("Get all category")
    public ResponseEntity<List<Category>> getAllProducts() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.getAllCategories());
    }

    @GetMapping("/category/{id}")
    @ApiMessage("Get category by id")
    public ResponseEntity<Category> getCategoryById(@PathVariable("id") Long id) throws IdInvalidException {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            throw new IdInvalidException("Product with id = " + id + " not found");
        }
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping("/category")
    @ApiMessage("Add new category")
    public ResponseEntity<Category> addCategory(@RequestBody Category category) throws IdInvalidException {
        if(category.getName() == null){
            throw new IdInvalidException("Category name is null");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.saveCategory(category));
    }

    @PutMapping("/category")
    @ApiMessage("Update category")
    public ResponseEntity<Category> updateCategory(@RequestBody Category category) throws IdInvalidException {
        if(categoryService.getCategoryById(category.getId()) == null){
            throw new IdInvalidException("Category ID is required");
        }
        if(category.getName() == null){
            throw new IdInvalidException("Category name is required");
        }
        return ResponseEntity.ok(categoryService.saveCategory(category));
    }

    @DeleteMapping("/category/{id}")
    @ApiMessage("Delete category")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Long id) throws IdInvalidException {
        if(categoryService.getCategoryById(id) == null) {
            throw new IdInvalidException("Category with id = " + id + " not found");
        }
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().body(null);
    }
}
