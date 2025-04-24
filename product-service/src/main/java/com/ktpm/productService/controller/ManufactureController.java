package com.ktpm.productService.controller;

import com.ktpm.productService.model.Category;
import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.service.CategoryService;
import com.ktpm.productService.service.ManufactureService;
import com.ktpm.productService.service.ProductService;
import com.ktpm.productService.utils.annotation.ApiMessage;
import com.ktpm.productService.utils.error.IdInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
public class ManufactureController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ManufactureService manufactureService;

    public ManufactureController(ProductService productService, CategoryService categoryService, ManufactureService manufactureService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.manufactureService = manufactureService;
    }

    @GetMapping("/manufacture")
    @ApiMessage("Get all manufacture")
    public ResponseEntity<List<Manufacture>> getAllManufacture() {
        return ResponseEntity.status(HttpStatus.OK).body(manufactureService.getAllManufacture());
    }

    @GetMapping("/manufacture/{id}")
    @ApiMessage("Get manufacture by id")
    public ResponseEntity<Manufacture> getManufactureById(@PathVariable("id") String id) throws IdInvalidException {
        Manufacture manufacture = manufactureService.getManufactureById(id);
        if (manufacture == null) {
            throw new IdInvalidException("Manufacture with id = " + id + " not found");
        }
        return ResponseEntity.ok(manufactureService.getManufactureById(id));
    }

    @PostMapping("/manufacture")
    @ApiMessage("Add new manufacture")
    public ResponseEntity<Manufacture> addManufacture(@RequestBody Manufacture manufacture) throws IdInvalidException {
        if(manufacture.getName() == null){
            throw new IdInvalidException("Manufacture name is null");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(manufactureService.saveManufacture(manufacture));
    }

    @PutMapping("/manufacture")
    @ApiMessage("Update manufacture")
    public ResponseEntity<Manufacture> updateManufacture(@RequestBody Manufacture manufacture) throws IdInvalidException {
        if(manufactureService.getManufactureById(manufacture.getId()) == null){
            throw new IdInvalidException("Manufacture ID is required");
        }
        if(manufacture.getName() == null){
            throw new IdInvalidException("Manufacture name is required");
        }
        return ResponseEntity.ok(manufactureService.saveManufacture(manufacture));
    }

    @DeleteMapping("/manufacture/{id}")
    @ApiMessage("Delete manufacture")
    public ResponseEntity<Void> deleteManufacture(@PathVariable("id") String id) throws IdInvalidException {
        if(manufactureService.getManufactureById(id) == null) {
            throw new IdInvalidException("Manufacture with id = " + id + " not found");
        }
        manufactureService.deleteManufacture(id);
        return ResponseEntity.ok().body(null);
    }
}
