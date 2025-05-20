package com.ktpm.productService.service;

import com.ktpm.productService.model.Image;
import com.ktpm.productService.repository.ImageRepository;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    public void deleteImageById(Long id) {
        imageRepository.deleteById(id);
    }
}
