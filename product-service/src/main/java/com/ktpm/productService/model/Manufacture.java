package com.ktpm.productService.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Entity
@Table(name = "manufacture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Manufacture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "manufacture")
    private List<Product> products;
}
