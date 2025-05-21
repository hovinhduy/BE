package com.ktpm.productService.repository;

import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategory(Category category);
    @Query("SELECT p FROM Product p WHERE p.category = :category")
    Page<Product> findByCategory(@Param("category") Category category, Specification<Product> productSpecification, Pageable pageable);
    List<Product> findByManufacture(Manufacture manufacture);
    @Query("SELECT p FROM Product p WHERE p.manufacture = :manufacture")
    Page<Product> findByManufacture(@Param("manufacture") Manufacture manufacture, Specification<Product> productSpecification, Pageable pageable);
}
