package com.ktpm.productService.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private String name;
    private double price;
    private String shortDesc;
    private String detailDesc;
    private int quantity;
    private Long categoryId;
    private Long manufactureId;
}
