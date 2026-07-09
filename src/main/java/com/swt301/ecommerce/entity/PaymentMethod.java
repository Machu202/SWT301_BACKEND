// Vị trí: src/main/java/com/swt301/ecommerce/entity/PaymentMethod.java
package com.swt301.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_method_id")
    private Integer paymentMethodId;

    @Column(name = "method_name", nullable = false)
    private String methodName; // VD: "COD", "QR_CODE"
}