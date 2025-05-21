package com.ktpm.productService.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private String shortDesc;
    private String detailDesc;
    private Integer quantity;
    private List<ImageDTO> images;
    private CategoryDTO category;
    private ManufactureDTO manufacture;
    private Integer sold;
    private Long categoryId;
    private Long manufactureId;
}
