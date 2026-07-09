// Vị trí: src/main/java/com/swt301/ecommerce/repository/OrderRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
}