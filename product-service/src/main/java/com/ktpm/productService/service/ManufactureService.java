package com.ktpm.productService.service;

import com.ktpm.productService.model.Manufacture;
import com.ktpm.productService.repository.ManufactureRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManufactureService {
    private final ManufactureRepository manufactureRepository;

    public ManufactureService(ManufactureRepository manufactureRepository) {
        this.manufactureRepository = manufactureRepository;
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
        manufactureRepository.deleteById(id);
    }
}
