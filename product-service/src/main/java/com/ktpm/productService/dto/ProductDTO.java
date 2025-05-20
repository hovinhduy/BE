package com.ktpm.productService.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private String shortDesc;
    private String detailDesc;
    private Integer quantity;
    private Long categoryId;
    private Long manufactureId;
    private Integer sold;
}
