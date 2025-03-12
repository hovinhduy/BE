package com.ktpm.productService.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private double price;

    @Column(name = "image")
    private int image;

    @Column(name = "detailDesc")
    private String detailDesc;

    @Column(name = "shortDesc")
    private String shortDesc;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "soid")
    private int soid;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "manufacture_id")
    private Manufacture manufacture;
}
