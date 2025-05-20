package com.ktpm.productService.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ktpm.productService.model.Image;
import com.ktpm.productService.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UploadService {
    private final Cloudinary cloudinary;
    private final ImageRepository imageRepository;

    public UploadService(Cloudinary cloudinary, ImageRepository imageRepository) {
        this.cloudinary = cloudinary;
        this.imageRepository = imageRepository;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return (String) uploadResult.get("secure_url");
    }

    public List<Image> uploadManyFiles(List<MultipartFile> files){
        List<Image> images = new ArrayList<>();
        files.forEach(
            file -> {
                try {
                    String url = uploadFile(file);
                    Image image = new Image();
                    image.setUrl(url);
                    images.add(imageRepository.save(image));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        );
        return images;
    }
}
