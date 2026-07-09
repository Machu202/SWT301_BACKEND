// Vị trí: src/main/java/com/swt301/ecommerce/repository/OrderItemRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_OrderId(Integer orderId);
}