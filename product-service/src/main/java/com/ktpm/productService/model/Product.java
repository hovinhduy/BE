package com.ktpm.productService.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Double price;

    @Column(name = "image")
    private String image;

    @Column(name = "detailDesc")
    private String detailDesc;

    @Column(name = "shortDesc")
    private String shortDesc;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "sold")
    private Integer sold;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "manufacture_id")
    private Manufacture manufacture;

    public Product() {
    }

    public Product(String name, Double price, String image, String detailDesc, String shortDesc, Integer quantity, Integer sold, Category category, Manufacture manufacture){
        this.name = name;
        this.price = price;
        this.image = image;
        this.detailDesc = detailDesc;
        this.shortDesc = shortDesc;
        this.quantity = quantity;
        this.sold = sold;
        this.category = category;
        this.manufacture = manufacture;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id=id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDetailDesc() {
        return detailDesc;
    }

    public void setDetailDesc(String detailDesc) {
        this.detailDesc = detailDesc;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Manufacture getManufacture() {
        return manufacture;
    }

    public void setManufacture(Manufacture manufacture) {
        this.manufacture = manufacture;
    }
}
