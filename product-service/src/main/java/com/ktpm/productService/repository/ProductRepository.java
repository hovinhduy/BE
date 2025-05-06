package com.ktpm.productService.repository;

import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategory(Category category);
    List<Product> findByManufacture(Manufacture manufacture);
}
