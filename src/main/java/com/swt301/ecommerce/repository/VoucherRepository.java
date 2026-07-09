// Vị trí: src/main/java/com/swt301/ecommerce/repository/VoucherRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByVoucherCode(String voucherCode);
}