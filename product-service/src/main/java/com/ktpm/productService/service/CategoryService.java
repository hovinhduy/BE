package com.ktpm.productService.service;

import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.repository.CategoryRepository;
import com.ktpm.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id).orElse(null);
        List<Product> product = productRepository.findByCategory(category);
        productRepository.deleteAll(product);
        categoryRepository.delete(category);
    }
}
