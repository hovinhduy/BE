package com.ktpm.productService.controller;

import com.ktpm.productService.dto.ProductDTO;
import com.ktpm.productService.dto.response.RestResponse;
import com.ktpm.productService.dto.response.ResultPaginationDTO;
import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.service.CategoryService;
import com.ktpm.productService.service.ManufactureService;
import com.ktpm.productService.service.ProductService;
import com.ktpm.productService.service.UploadService;
import com.ktpm.productService.utils.annotation.ApiMessage;
import com.ktpm.productService.utils.error.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ManufactureService manufactureService;
    private final UploadService uploadService;

    public ProductController(ProductService productService, CategoryService categoryService,
            ManufactureService manufactureService, UploadService uploadService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.manufactureService = manufactureService;
        this.uploadService = uploadService;
    }

    @GetMapping("/product")
    @ApiMessage("Get all products")
    public ResponseEntity<ResultPaginationDTO> getAllProducts(
            @Filter Specification<Product> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProducts(spec, pageable));
    }

    @GetMapping("/product/{id}")
    @ApiMessage("Get product by id")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) throws IdInvalidException {
        Product prouduct = productService.getProductById(id);
        if (prouduct == null) {
            throw new IdInvalidException("Product with id = " + id + " not found");
        }
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(value = "/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Add new product")
    public ResponseEntity<Product> addProduct(
            @ModelAttribute("product") ProductDTO productDto,
            @RequestPart(value = "file", required = false) MultipartFile imageFile)
            throws IdInvalidException, IOException {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setShortDesc(productDto.getShortDesc());
        product.setDetailDesc(productDto.getDetailDesc());
        product.setQuantity(productDto.getQuantity());

        if (productDto.getCategoryId() == null) {
            throw new IdInvalidException("Category ID is required");
        }

        Category category = categoryService.getCategoryById(productDto.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Category with id = " + productDto.getCategoryId() + " not found");
        }
        product.setCategory(category);

        if (productDto.getManufactureId() == null) {
            throw new IdInvalidException("Manufacture ID is required");
        }
        Manufacture manufacture = manufactureService.getManufactureById(productDto.getManufactureId());
        if (manufacture == null) {
            throw new IdInvalidException("Manufacture with id = " + productDto.getManufactureId() + " not found");
        }
        product.setManufacture(manufacture);

        String imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = uploadService.uploadFile(imageFile);
        } else {
            imageUrl = "https://i.ibb.co/TDvW7DKg/pepe-the-frog-1272162-640.jpg";
        }
        product.setImage(imageUrl);
        product.setSold(0);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(product));
    }

    @PutMapping(value = "/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Update product")
    public ResponseEntity<Product> updateProduct(
            @ModelAttribute("product") ProductDTO productDto,
            @RequestPart(value = "file", required = false) MultipartFile imageFile)
            throws IdInvalidException, IOException {
        if (productDto.getId() == null) {
            throw new IdInvalidException("Product ID is required");
        }
        Product product = productService.getProductById(productDto.getId());

        if (product == null) {
            throw new IdInvalidException("Product with id = " + productDto.getId() + " not found");
        }

        product.setId(productDto.getId());
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setShortDesc(productDto.getShortDesc());
        product.setDetailDesc(productDto.getDetailDesc());
        product.setQuantity(productDto.getQuantity());
        product.setSold(productDto.getSold());

        // Kiểm tra và gán lại Manufacture nếu có
        if (productDto.getManufactureId() != null) {
            Long id = productDto.getManufactureId();
            Manufacture manufacture = manufactureService.getManufactureById(id);
            if (manufacture == null) {
                throw new IdInvalidException("Manufacture with id = " + id + " not found");
            }
            product.setManufacture(manufacture);
        } else {
            product.setManufacture(product.getManufacture());
        }

        // Kiểm tra và gán lại Category nếu có
        if (productDto.getCategoryId() != null) {
            Long id = productDto.getCategoryId();
            Category category = categoryService.getCategoryById(id);
            if (category == null) {
                throw new IdInvalidException("Category with id = " + id + " not found");
            }
            product.setCategory(category);
        } else {
            product.setCategory(product.getCategory());
        }

        // Xử lý cập nhật hình ảnh
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = uploadService.uploadFile(imageFile);
            product.setImage(imageUrl);
        } else {
            // Giữ nguyên ảnh cũ nếu không upload ảnh mới
            product.setImage(product.getImage());
        }
        return ResponseEntity.ok(productService.updateProduct(product));
    }

    @DeleteMapping("/product/{id}")
    @ApiMessage("Delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) throws IdInvalidException {
        if (productService.getProductById(id) == null) {
            throw new IdInvalidException("Product with id = " + id + " not found");
        }
        productService.deleteProduct(id);
        return ResponseEntity.ok().body(null);
    }
}
