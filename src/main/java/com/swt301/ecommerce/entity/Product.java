// Vị trí: src/main/java/com/swt301/ecommerce/entity/Product.java
package com.swt301.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price; // Luôn dùng BigDecimal cho tiền bạc

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "image")
    private String image;
    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";
}