// Vị trí: src/main/java/com/swt301/ecommerce/repository/PaymentRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByOrder_OrderId(Integer orderId);
}