// Vị trí: src/main/java/com/swt301/ecommerce/entity/Payment.java
package com.swt301.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "qr_image")
    private String qrImage;

    @Column(name = "payment_status")
    private String paymentStatus; 

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
}