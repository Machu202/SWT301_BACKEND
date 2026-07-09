// Vị trí: src/main/java/com/swt301/ecommerce/repository/PaymentMethodRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    Optional<PaymentMethod> findByMethodName(String methodName);
}