// Vị trí: src/main/java/com/swt301/ecommerce/repository/OrderStatusRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Integer> {
    Optional<OrderStatus> findByStatusName(String statusName);
}