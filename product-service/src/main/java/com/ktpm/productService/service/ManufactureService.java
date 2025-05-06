package com.ktpm.productService.service;

import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.model.Product;
import com.ktpm.productService.repository.ManufactureRepository;
import com.ktpm.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManufactureService {
    private final ManufactureRepository manufactureRepository;
    private final ProductRepository productRepository;

    public ManufactureService(ManufactureRepository manufactureRepository, ProductRepository productRepository) {
        this.manufactureRepository = manufactureRepository;
        this.productRepository = productRepository;
    }

    public List<Manufacture> getAllManufacture() {
        return manufactureRepository.findAll();
    }

    public Manufacture getManufactureById(Long id) {
        return manufactureRepository.findById(id).orElse(null);
    }

    public Manufacture saveManufacture(Manufacture manufacture) {
        return manufactureRepository.save(manufacture);
    }

    public void deleteManufacture(Long id) {
        Manufacture manufacture = manufactureRepository.findById(id).orElse(null);
        List<Product> product = productRepository.findByManufacture(manufacture);
        productRepository.deleteAll(product);
        manufactureRepository.delete(manufacture);
    }
}
