// Vị trí: src/main/java/com/swt301/ecommerce/entity/Voucher.java
package com.swt301.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "voucher_code", unique = true, nullable = false)
    private String voucherCode;

    @Column(name = "voucher_name")
    private String voucherName;

    @Column(name = "discount_type")
    private String discountType; // "PERCENT" hoặc "FIXED"

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "min_order")
    private BigDecimal minOrder;

    @Column(name = "expired_date")
    private LocalDateTime expiredDate;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "status")
    private String status; // "ACTIVE" hoặc "INACTIVE"
}