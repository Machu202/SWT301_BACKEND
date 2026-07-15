package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_OrderId(Integer orderId);

    @EntityGraph(attributePaths = {"product", "order"})
    List<OrderItem> findByOrder_OrderIdIn(Collection<Integer> orderIds);
}
