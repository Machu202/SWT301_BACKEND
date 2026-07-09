// Vị trí: src/main/java/com/swt301/ecommerce/entity/OrderStatus.java
package com.swt301.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", nullable = false)
    private String statusName; // VD: "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
}